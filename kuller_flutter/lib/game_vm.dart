import 'package:flutter/foundation.dart';

import 'content.dart';
import 'dialogue_factory.dart';
import 'game_state.dart';
import 'models.dart';
import 'order_factory.dart';
import 'tts.dart';

/// Текущая сессия доставки (мутабельная; VM уведомляет слушателей).
class OrderSession {
  final Order order;
  final Map<String, Phase> phases;
  String phaseId;
  List<Turn> turns;
  int turnIndex = 0;
  final List<ChatMsg> transcript = [];
  ChatThread activeThread;
  final Set<ChatThread> threadsSeen;
  final Set<ChatThread> unread = {};
  bool awaiting = false;
  NavChoose? nav;
  bool atEnd = false;
  ChatThread? typing;
  int? selected;
  int correctCount = 0;
  int mistakes = 0;
  double ratingBonus = 0.0;
  bool finished = false;

  OrderSession({
    required this.order,
    required this.phases,
    required this.phaseId,
    required this.turns,
    required this.activeThread,
  }) : threadsSeen = {activeThread};

  Ask? get currentAsk {
    final t = turnIndex < turns.length ? turns[turnIndex] : null;
    return t is Ask ? t : null;
  }

  double get progress => turns.isEmpty ? 0.0 : turnIndex / turns.length;

  List<ChatMsg> messages(ChatThread thread) =>
      transcript.where((m) => m.thread == thread).toList();

  List<ChatThread> get tabs =>
      threadOrder.where((t) => threadsSeen.contains(t)).toList();
}

class DeliveryResult {
  final Order order;
  final int steps;
  final int mistakes;
  final double earned;
  final double tip;
  final int xp;
  final bool perfect;
  DeliveryResult({
    required this.order,
    required this.steps,
    required this.mistakes,
    required this.earned,
    required this.tip,
    required this.xp,
    required this.perfect,
  });

  String get earnedStr => '${earned.toStringAsFixed(2)} €';
  String get tipStr => '${tip.toStringAsFixed(2)} €';
}

class GameViewModel extends ChangeNotifier {
  static const mastery = 20;

  final GameRepository repo;
  final Speaker tts = Speaker();

  GameState state;
  bool online = false;
  OrderSession? session;
  List<Order> availableOrders = [];
  DeliveryResult? lastResult;

  /// Глобальный переключатель: скрывать ли русские переводы в чатах.
  bool hideRu;

  int _revealGen = 0;

  GameViewModel(this.repo)
      : state = repo.load(),
        hideRu = repo.loadHideRu();

  void toggleHideRu() {
    hideRu = !hideRu;
    repo.saveHideRu(hideRu);
    notifyListeners();
  }

  void toggleOnline() {
    online = !online;
    if (online) {
      availableOrders = OrderFactory.batch(5);
    }
    notifyListeners();
  }

  void refreshOrders() {
    availableOrders = OrderFactory.batch(5);
    notifyListeners();
  }

  /// Тон голоса по роли.
  double _pitchOf(ChatThread thread, bool fromCourier) {
    if (fromCourier) return 1.0;
    switch (thread) {
      case ChatThread.klient:
        return 1.25;
      case ChatThread.restoran:
        return 0.8;
      case ChatThread.tugi:
        return 1.1;
    }
  }

  /// Озвучить вручную (по кнопке 🔊) голосом нужного собеседника.
  void speak(String text, bool fromCourier, ChatThread thread) =>
      tts.speak(text, pitch: _pitchOf(thread, fromCourier));

  void startOrder(Order order) {
    _revealGen++;
    final delivery = DialogueFactory.build(order);
    // Перемешиваем варианты во всех фазах.
    final phases = <String, Phase>{};
    delivery.phases.forEach((id, ph) {
      phases[id] = Phase(
        ph.id,
        ph.thread,
        ph.turns
            .map((t) => t is Ask ? t.withChoices([...t.choices]..shuffle()) : t)
            .toList(),
        ph.nav,
      );
    });
    final start = phases[delivery.start]!;
    session = OrderSession(
      order: order,
      phases: phases,
      phaseId: delivery.start,
      turns: List.of(start.turns),
      activeThread: start.thread,
    );
    lastResult = null;
    notifyListeners();
    _revealUntilAsk();
  }

  Future<void> _revealUntilAsk() async {
    final gen = ++_revealGen;
    while (true) {
      final s = session;
      if (s == null || gen != _revealGen) return;
      final turn = s.turnIndex < s.turns.length ? s.turns[s.turnIndex] : null;
      if (turn == null) {
        // Фаза закончилась → навигация.
        final nav = s.phases[s.phaseId]?.nav;
        if (nav is NavChoose) {
          s.nav = nav;
          s.typing = null;
          s.awaiting = false;
        } else {
          s.atEnd = true;
          s.typing = null;
          s.awaiting = false;
          s.nav = null;
        }
        notifyListeners();
        return;
      }
      if (turn is Ask) {
        s.awaiting = true;
        s.typing = null;
        notifyListeners();
        return;
      }
      final say = turn as Say;
      s.typing = say.thread;
      notifyListeners();
      await Future.delayed(const Duration(milliseconds: 1000));
      if (session != s || gen != _revealGen) return;
      s.transcript
          .add(ChatMsg(say.thread, false, say.et, say.ru, threadLabel(say.thread)));
      s.turnIndex++;
      s.typing = null;
      if (say.thread != s.activeThread) s.unread.add(say.thread);
      notifyListeners();
      tts.speak(say.et, pitch: _pitchOf(say.thread, false));
      await Future.delayed(const Duration(milliseconds: 300));
      if (session != s || gen != _revealGen) return;
    }
  }

  void setActiveThread(ChatThread thread) {
    final s = session;
    if (s == null) return;
    s.activeThread = thread;
    s.unread.remove(thread);
    notifyListeners();
  }

  void select(int index) {
    final s = session;
    if (s == null || !s.awaiting) return;
    s.selected = index;
    notifyListeners();
  }

  void confirm() {
    final s = session;
    if (s == null || !s.awaiting) return;
    final ask = s.currentAsk;
    final i = s.selected;
    if (ask == null || i == null) return;
    final choice = ask.choices[i];

    s.transcript.add(ChatMsg(ask.thread, true, choice.et, choice.ru, '🛵 Вы'));
    Map<String, int> newReps = state.reps;
    if (choice.correct && ask.teachWordIds.isNotEmpty) {
      newReps = Map.of(state.reps);
      for (final id in ask.teachWordIds) {
        newReps[id] = (newReps[id] ?? 0) + 1;
      }
    }
    state = state.copyWith(
      learnedIds: choice.correct
          ? {...state.learnedIds, ...ask.teachWordIds}
          : state.learnedIds,
      correct: state.correct + (choice.correct ? 1 : 0),
      wrong: state.wrong + (choice.correct ? 0 : 1),
      reps: newReps,
    );
    repo.save(state);

    // Любой вариант рабочий: вставляем его ветку и идём дальше.
    s.turns = [
      ...s.turns.sublist(0, s.turnIndex + 1),
      ...choice.followUp,
      ...s.turns.sublist(s.turnIndex + 1),
    ];
    s.turnIndex++;
    s.awaiting = false;
    s.selected = null;
    if (choice.correct) {
      s.correctCount++;
    } else {
      s.mistakes++;
    }
    s.ratingBonus += choice.ratingDelta;
    notifyListeners();
    tts.speak(choice.et, pitch: _pitchOf(ask.thread, true));
    _revealUntilAsk();
  }

  /// Курьер выбрал, что делать дальше (какой чат открыть).
  void navChoose(NavOption option) {
    final s = session;
    if (s == null) return;
    final phase = s.phases[option.phase];
    if (phase == null) return;
    s.phaseId = option.phase;
    s.turns = List.of(phase.turns);
    s.turnIndex = 0;
    s.activeThread = phase.thread;
    s.threadsSeen.add(phase.thread);
    s.unread.remove(phase.thread);
    s.nav = null;
    s.awaiting = false;
    s.selected = null;
    notifyListeners();
    _revealUntilAsk();
  }

  void finishDelivery() {
    final s = session;
    if (s == null || !s.atEnd) return;
    final perfect = s.mistakes == 0;
    final tip = perfect ? s.order.payout * 0.25 : 0.0;
    final earned = s.order.payout + tip;
    final xpGain = s.correctCount * 20 + (perfect ? 30 : 0);
    final baseDelta = perfect ? 0.02 : -0.02 * s.mistakes;
    final newRating =
        (state.rating + baseDelta + s.ratingBonus).clamp(3.0, 5.0).toDouble();

    state = state.copyWith(
      money: state.money + earned,
      xp: state.xp + xpGain,
      deliveries: state.deliveries + 1,
      rating: newRating,
    );
    repo.save(state);
    lastResult = DeliveryResult(
      order: s.order,
      steps: s.correctCount,
      mistakes: s.mistakes,
      earned: earned,
      tip: tip,
      xp: xpGain,
      perfect: perfect,
    );
    s.finished = true;
    notifyListeners();
  }

  void cancelOrder() {
    _revealGen++;
    session = null;
    notifyListeners();
  }

  void closeResult() {
    session = null;
    lastResult = null;
    if (online) {
      availableOrders = OrderFactory.batch(5);
    }
    notifyListeners();
  }

  Future<void> resetProgress() async {
    _revealGen++;
    await repo.clear();
    state = const GameState();
    session = null;
    lastResult = null;
    online = false;
    notifyListeners();
  }

  int learnedInCategory(String categoryId) {
    final cat = Content.categories.where((c) => c.id == categoryId).toList();
    if (cat.isEmpty) return 0;
    final ids = cat.first.words.map((w) => w.id).toSet();
    return state.learnedIds.where(ids.contains).length;
  }

  String moneyStr() => '${state.money.toStringAsFixed(2)} €';
  String ratingStr() => state.rating.toStringAsFixed(2);

  /// Скрывает в русском тексте перевод тех слов, что отработаны ≥ [mastery]
  /// раз (заменяет на «…»), чтобы вспоминать значение самому.
  String maskRu(String text) {
    if (state.reps.isEmpty) return text;
    var s = text;
    for (final entry in state.reps.entries) {
      if (entry.value < mastery) continue;
      final w = Content.word(entry.key);
      if (w == null) continue;
      final glosses = w.ru
          .split(RegExp(r'[/,]'))
          .map((g) => g.trim())
          .where((g) => g.length >= 3);
      for (final gloss in glosses) {
        final p = RegExp(
          r'(?<![\p{L}])' + RegExp.escape(gloss) + r'\p{L}*',
          caseSensitive: false,
          unicode: true,
        );
        s = s.replaceAll(p, '…');
      }
    }
    return s;
  }

  @override
  void dispose() {
    _revealGen++;
    tts.shutdown();
    super.dispose();
  }
}
