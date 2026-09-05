import 'dart:math';

import 'package:flutter/material.dart';

import '../learn_vm.dart';
import '../models.dart';
import '../widgets.dart';

class LessonScreen extends StatelessWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const LessonScreen({super.key, required this.vm, required this.lesson});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final tabs = <Tab>[
      const Tab(text: 'Слова'),
      if (lesson.texts.isNotEmpty) const Tab(text: 'Тексты'),
      if (lesson.dialogues.isNotEmpty) const Tab(text: 'Диалоги'),
      const Tab(text: 'Тест'),
    ];
    return DefaultTabController(
      length: tabs.length,
      child: Scaffold(
        backgroundColor: scheme.surfaceContainerLowest,
        appBar: AppBar(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          title: Text('${lesson.emoji} ${lesson.title}',
              style:
                  const TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
          actions: [
            ListenableBuilder(
              listenable: vm,
              builder: (context, _) => IconButton(
                tooltip: 'Скрыть/показать переводы',
                onPressed: vm.toggleHideTr,
                icon: Icon(
                    vm.hideTr ? Icons.visibility_off : Icons.visibility,
                    color: scheme.onPrimary),
              ),
            ),
          ],
          bottom: TabBar(
            tabs: tabs,
            labelColor: scheme.onPrimary,
            unselectedLabelColor: scheme.onPrimary.withOpacity(0.6),
            indicatorColor: scheme.onPrimary,
          ),
        ),
        body: ListenableBuilder(
          listenable: vm,
          builder: (context, _) => TabBarView(
            children: [
              _WordsTab(vm: vm, lesson: lesson),
              if (lesson.texts.isNotEmpty) _TextsTab(vm: vm, lesson: lesson),
              if (lesson.dialogues.isNotEmpty)
                _DialoguesTab(vm: vm, lesson: lesson),
              TestTab(vm: vm, lesson: lesson),
            ],
          ),
        ),
      ),
    );
  }
}

// ============================ СЛОВА ============================

class _WordsTab extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _WordsTab({required this.vm, required this.lesson});

  @override
  State<_WordsTab> createState() => _WordsTabState();
}

class _WordsTabState extends State<_WordsTab> {
  final Set<int> revealed = {};

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: widget.lesson.words.length,
      itemBuilder: (context, i) {
        final w = widget.lesson.words[i];
        final learned = vm.state.learnedWords.contains(w.et);
        final showTr = !vm.hideTr || revealed.contains(i);
        return Card(
          elevation: 1,
          margin: const EdgeInsets.symmetric(vertical: 4),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          color: scheme.surface,
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: vm.hideTr
                ? () => setState(() =>
                    revealed.contains(i) ? revealed.remove(i) : revealed.add(i))
                : null,
            child: Padding(
              padding: const EdgeInsets.all(12).copyWith(left: 14),
              child: Row(
                children: [
                  if (w.emoji.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(right: 10),
                      child: Text(w.emoji, style: const TextStyle(fontSize: 24)),
                    ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Flexible(
                              child: Text(w.et,
                                  style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                      color: scheme.onSurface)),
                            ),
                            if (learned)
                              const Padding(
                                padding: EdgeInsets.only(left: 6),
                                child: Text('✅', style: TextStyle(fontSize: 12)),
                              ),
                          ],
                        ),
                        Text(showTr ? w.tr : '••• (нажмите, чтобы открыть)',
                            style: TextStyle(
                                fontSize: 13,
                                color: scheme.onSurface
                                    .withOpacity(showTr ? 0.7 : 0.4))),
                        if (w.example.isNotEmpty)
                          Text('„${w.example}“',
                              style: TextStyle(
                                  fontSize: 12, color: scheme.primary)),
                      ],
                    ),
                  ),
                  SpeakButton(onPressed: () => vm.speakWord(w.et)),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

// ============================ ТЕКСТЫ ============================

class _TextsTab extends StatelessWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _TextsTab({required this.vm, required this.lesson});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Text('Тексты урока — читайте и слушайте целиком или по абзацам.',
            style: TextStyle(
                fontSize: 13, color: scheme.onSurface.withOpacity(0.65))),
        const SizedBox(height: 10),
        for (final t in lesson.texts)
          Card(
            elevation: 1,
            margin: const EdgeInsets.symmetric(vertical: 5),
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            color: scheme.surface,
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => TextReaderScreen(vm: vm, text: t))),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    const Text('📖', style: TextStyle(fontSize: 22)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(t.title,
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: scheme.onSurface)),
                          Text(
                              '${t.paras.length} абзацев'
                              '${t.questions.isNotEmpty ? ' · ${t.questions.length} вопросов' : ''}',
                              style: TextStyle(
                                  fontSize: 12,
                                  color: scheme.onSurface.withOpacity(0.6))),
                        ],
                      ),
                    ),
                    Icon(Icons.headphones, color: scheme.primary, size: 28),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

/// Читалка текста: чтение + прослушивание целиком (с подсветкой абзаца).
class TextReaderScreen extends StatefulWidget {
  final LearnViewModel vm;
  final LessonText text;
  const TextReaderScreen({super.key, required this.vm, required this.text});

  @override
  State<TextReaderScreen> createState() => _TextReaderScreenState();
}

class _TextReaderScreenState extends State<TextReaderScreen> {
  bool playing = false;
  int currentPara = -1;
  int _gen = 0;
  final Set<int> openAnswers = {};
  late final List<GlobalKey> _paraKeys =
      List.generate(widget.text.paras.length, (_) => GlobalKey());

  LearnViewModel get vm => widget.vm;

  @override
  void dispose() {
    _gen++;
    vm.tts.shutdown();
    super.dispose();
  }

  Future<void> _playAll({int from = 0}) async {
    final gen = ++_gen;
    setState(() => playing = true);
    for (var i = from; i < widget.text.paras.length; i++) {
      if (!mounted || gen != _gen) return;
      setState(() => currentPara = i);
      final ctx = _paraKeys[i].currentContext;
      if (ctx != null) {
        Scrollable.ensureVisible(ctx,
            duration: const Duration(milliseconds: 300), alignment: 0.2);
      }
      await vm.tts.speakAwait(widget.text.paras[i].et);
      if (!mounted || gen != _gen) return;
      await Future.delayed(const Duration(milliseconds: 250));
    }
    if (!mounted || gen != _gen) return;
    setState(() {
      playing = false;
      currentPara = -1;
    });
  }

  void _stop() {
    _gen++;
    vm.tts.shutdown();
    setState(() {
      playing = false;
      currentPara = -1;
    });
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = widget.text;
    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        title: Text('📖 ${text.title}',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
        actions: [
          ListenableBuilder(
            listenable: vm,
            builder: (context, _) => IconButton(
              tooltip: 'Скрыть/показать переводы',
              onPressed: vm.toggleHideTr,
              icon: Icon(vm.hideTr ? Icons.visibility_off : Icons.visibility,
                  color: scheme.onPrimary),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: playing ? _stop : () => _playAll(),
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        icon: Icon(playing ? Icons.stop : Icons.play_arrow),
        label: Text(playing ? 'Стоп' : 'Слушать весь текст'),
      ),
      body: ListenableBuilder(
        listenable: vm,
        builder: (context, _) => ListView(
          padding: const EdgeInsets.fromLTRB(14, 14, 14, 90),
          children: [
            for (var i = 0; i < text.paras.length; i++)
              Card(
                key: _paraKeys[i],
                elevation: 0,
                margin: const EdgeInsets.symmetric(vertical: 4),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                  side: currentPara == i
                      ? BorderSide(color: scheme.primary, width: 2)
                      : BorderSide.none,
                ),
                color: currentPara == i
                    ? scheme.primaryContainer.withOpacity(0.45)
                    : scheme.surface,
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(text.paras[i].et,
                                style: TextStyle(
                                    fontSize: 16,
                                    height: 1.4,
                                    color: scheme.onSurface)),
                            if (text.paras[i].tr.isNotEmpty && !vm.hideTr)
                              Padding(
                                padding: const EdgeInsets.only(top: 5),
                                child: Text(text.paras[i].tr,
                                    style: TextStyle(
                                        fontSize: 13.5,
                                        height: 1.35,
                                        color: scheme.onSurface
                                            .withOpacity(0.6))),
                              ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 6),
                      SpeakButton(
                          onPressed: () {
                            _gen++;
                            setState(() {
                              playing = false;
                              currentPara = -1;
                            });
                            vm.speakWord(text.paras[i].et);
                          }),
                    ],
                  ),
                ),
              ),
            if (text.questions.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('❓ Проверьте себя',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                      color: scheme.onSurface)),
              const SizedBox(height: 4),
              for (var qi = 0; qi < text.questions.length; qi++)
                Builder(builder: (context) {
                  final qa = text.questions[qi];
                  final open = openAnswers.contains(qi);
                  return Card(
                    elevation: 0,
                    margin: const EdgeInsets.symmetric(vertical: 3),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                    color: open
                        ? scheme.primaryContainer.withOpacity(0.5)
                        : scheme.surface,
                    child: InkWell(
                      borderRadius: BorderRadius.circular(12),
                      onTap: () => setState(() =>
                          open ? openAnswers.remove(qi) : openAnswers.add(qi)),
                      child: Padding(
                        padding: const EdgeInsets.all(10),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(qa.q,
                                style: TextStyle(
                                    fontWeight: FontWeight.w600,
                                    fontSize: 14,
                                    color: scheme.onSurface)),
                            Padding(
                              padding: const EdgeInsets.only(top: 3),
                              child: Text(open ? qa.a : 'Показать ответ…',
                                  style: TextStyle(
                                      fontSize: 13,
                                      color: open
                                          ? scheme.primary
                                          : scheme.onSurface
                                              .withOpacity(0.5))),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                }),
            ],
          ],
        ),
      ),
    );
  }
}

// ============================ ДИАЛОГИ ============================

class _DialoguesTab extends StatelessWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _DialoguesTab({required this.vm, required this.lesson});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Text('Диалог строится по ходу: собеседник пишет — вы выбираете ответ, '
            'и разговор развивается дальше. Каждый диалог проходится в два '
            'этапа: сначала за одну роль, потом за другую.',
            style: TextStyle(
                fontSize: 13, color: scheme.onSurface.withOpacity(0.65))),
        const SizedBox(height: 10),
        for (final d in lesson.dialogues)
          Card(
            elevation: 1,
            margin: const EdgeInsets.symmetric(vertical: 5),
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            color: scheme.surface,
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => DialoguePlayerScreen(vm: vm, dialogue: d))),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    const Text('💬', style: TextStyle(fontSize: 22)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(d.title,
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: scheme.onSurface)),
                          Text(
                              '2 этапа · ${d.turns.whereType<DAsk>().length} выборов реплик',
                              style: TextStyle(
                                  fontSize: 12,
                                  color: scheme.onSurface.withOpacity(0.6))),
                        ],
                      ),
                    ),
                    Icon(Icons.play_circle_fill,
                        color: scheme.primary, size: 32),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _Bubble {
  final bool fromUser;
  final String et;
  final String tr;
  _Bubble(this.fromUser, this.et, this.tr);
}

class DialoguePlayerScreen extends StatefulWidget {
  final LearnViewModel vm;
  final Dialogue dialogue;
  const DialoguePlayerScreen(
      {super.key, required this.vm, required this.dialogue});

  @override
  State<DialoguePlayerScreen> createState() => _DialoguePlayerScreenState();
}

class _DialoguePlayerScreenState extends State<DialoguePlayerScreen> {
  final List<_Bubble> bubbles = [];
  late List<DTurn> queue;
  DAsk? currentAsk;
  bool isRetry = false; // это повтор вопроса после ошибки?
  final Set<DAsk> _retries = {};
  bool typing = false;
  bool finished = false;
  int stage = 1; // 1 — вы отвечаете; 2 — вы за первого собеседника
  int asks = 0; // сколько вопросов встретили
  int firstTryCorrect = 0;
  int _gen = 0;
  final _scroll = ScrollController();
  final _rnd = Random();

  LearnViewModel get vm => widget.vm;

  @override
  void initState() {
    super.initState();
    _start(1);
  }

  void _start(int newStage) {
    _gen++;
    setState(() {
      stage = newStage;
      bubbles.clear();
      currentAsk = null;
      finished = false;
      typing = false;
      asks = 0;
      firstTryCorrect = 0;
      isRetry = false;
      _retries.clear();
      queue = newStage == 1
          ? List.of(widget.dialogue.turns)
          : _swapRoles(widget.dialogue.turns);
    });
    _advance();
  }

  /// Этап 2: роли меняются местами. Реплики первого собеседника становятся
  /// выбором ученика (неверные варианты — другие его реплики из этого же
  /// диалога), а верные ответы ученика озвучивает собеседник.
  List<DTurn> _swapRoles(List<DTurn> orig) {
    final pool = <DSay>[
      for (final t in orig)
        if (t is DSay) t,
      for (final t in orig)
        if (t is DAsk)
          for (final o in t.options)
            if (o.correct) ...o.followUp,
    ];
    final result = <DTurn>[];
    void addAsk(DSay line) {
      final distractors = pool.where((p) => p.et != line.et).toList()
        ..shuffle(_rnd);
      final opts = <DChoice>[
        DChoice(line.et, line.tr),
        for (final d in distractors.take(2))
          DChoice(d.et, d.tr, correct: false),
      ]..shuffle(_rnd);
      result.add(DAsk('Теперь вы — первый собеседник. Выберите реплику:', opts));
    }

    for (final t in orig) {
      if (t is DSay) addAsk(t);
      if (t is DAsk) {
        final correct = t.options.firstWhere((o) => o.correct,
            orElse: () => t.options.first);
        result.add(DSay(correct.et, correct.tr));
        for (final f in correct.followUp) {
          addAsk(f);
        }
      }
    }
    return result;
  }

  @override
  void dispose() {
    _gen++;
    _scroll.dispose();
    super.dispose();
  }

  void _autoScroll() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scroll.hasClients) return;
      _scroll.animateTo(_scroll.position.maxScrollExtent,
          duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
    });
  }

  Future<void> _advance() async {
    final gen = ++_gen;
    while (queue.isNotEmpty) {
      final turn = queue.removeAt(0);
      if (turn is DSay) {
        setState(() => typing = true);
        _autoScroll();
        await Future.delayed(const Duration(milliseconds: 700));
        if (!mounted || gen != _gen) return;
        setState(() {
          typing = false;
          bubbles.add(_Bubble(false, turn.et, turn.tr));
        });
        _autoScroll();
        // Ждём, пока фраза прозвучит целиком, и лишь потом идём дальше
        await vm.speakAwait(turn.et, speaker: 'A');
        if (!mounted || gen != _gen) return;
        await Future.delayed(const Duration(milliseconds: 250));
        if (!mounted || gen != _gen) return;
      } else if (turn is DAsk) {
        setState(() {
          currentAsk = turn;
          isRetry = _retries.remove(turn);
          if (!isRetry) asks++;
        });
        _autoScroll();
        return;
      }
    }
    setState(() => finished = true);
    _autoScroll();
  }

  Future<void> _choose(DChoice choice) async {
    final ask = currentAsk;
    if (ask == null) return;
    setState(() {
      bubbles.add(_Bubble(true, choice.et, choice.tr));
      currentAsk = null;
    });
    _autoScroll();
    if (choice.correct) {
      if (!isRetry) firstTryCorrect++;
      queue.insertAll(0, choice.followUp);
    } else {
      // Реакция собеседника + тот же вопрос ещё раз (без выбранного варианта)
      final remaining = ask.options.where((o) => o != choice).toList();
      final retry = DAsk(ask.prompt, remaining);
      _retries.add(retry);
      queue.insertAll(0, [...choice.followUp, retry]);
    }
    // Ваша реплика договаривается до конца, потом отвечает собеседник
    final gen = _gen;
    await vm.speakAwait(choice.et, speaker: 'B');
    if (!mounted || gen != _gen) return;
    _advance();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final ask = currentAsk;
    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        title: Text('💬 ${widget.dialogue.title} · этап $stage/2',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
        actions: [
          ListenableBuilder(
            listenable: vm,
            builder: (context, _) => IconButton(
              tooltip: 'Скрыть/показать переводы',
              onPressed: vm.toggleHideTr,
              icon: Icon(vm.hideTr ? Icons.visibility_off : Icons.visibility,
                  color: scheme.onPrimary),
            ),
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: vm,
        builder: (context, _) => Column(
          children: [
            Expanded(
              child: ListView(
                controller: _scroll,
                padding: const EdgeInsets.all(12),
                children: [
                  for (final b in bubbles) _bubbleWidget(context, b),
                  if (typing) _typingWidget(context),
                ],
              ),
            ),
            Material(
              color: scheme.surface,
              elevation: 8,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: finished
                    ? _finishPanel(context)
                    : (ask != null
                        ? _askPanel(context, ask)
                        : Padding(
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            child: Text('💬 …',
                                style: TextStyle(
                                    fontSize: 16,
                                    color:
                                        scheme.onSurface.withOpacity(0.5))),
                          )),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _askPanel(BuildContext context, DAsk ask) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(ask.prompt,
            style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14,
                color: scheme.onSurface)),
        const SizedBox(height: 8),
        ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 260),
          child: SingleChildScrollView(
            child: Column(
              children: [
                for (final o in ask.options)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Material(
                      color: scheme.surface,
                      borderRadius: BorderRadius.circular(14),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(14),
                        onTap: () => _choose(o),
                        child: Container(
                          decoration: BoxDecoration(
                            border: Border.all(
                                color: scheme.onSurface.withOpacity(0.15),
                                width: 2),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          padding: const EdgeInsets.all(12),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.start,
                                  children: [
                                    Text(o.et,
                                        style: TextStyle(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 15,
                                            color: scheme.onSurface)),
                                    if (!vm.hideTr && o.tr.isNotEmpty)
                                      Text(o.tr,
                                          style: TextStyle(
                                              fontSize: 12,
                                              color: scheme.onSurface
                                                  .withOpacity(0.65))),
                                  ],
                                ),
                              ),
                              SpeakButton(
                                  onPressed: () => vm.speakWord(o.et)),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _finishPanel(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          asks > 0
              ? 'Этап $stage пройден! С первой попытки: $firstTryCorrect из $asks 🎉'
              : 'Этап $stage пройден! 🎉',
          textAlign: TextAlign.center,
          style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 16,
              color: scheme.onSurface),
        ),
        if (stage == 1)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text('Теперь пройдите диалог за другую роль!',
                textAlign: TextAlign.center,
                style: TextStyle(
                    fontSize: 13, color: scheme.onSurface.withOpacity(0.7))),
          ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => _start(stage),
                child: const Text('Ещё раз'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: stage == 1
                  ? FilledButton(
                      onPressed: () => _start(2),
                      child: const Text('Этап 2 ▶',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                    )
                  : FilledButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Готово'),
                    ),
            ),
          ],
        ),
        if (stage == 2)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: OutlinedButton(
              onPressed: () => _start(1),
              child: const Text('Назад к этапу 1'),
            ),
          ),
      ],
    );
  }

  Widget _bubbleWidget(BuildContext context, _Bubble b) {
    final scheme = Theme.of(context).colorScheme;
    final bubbleColor =
        b.fromUser ? scheme.primary : scheme.surfaceContainerHighest;
    final onBubble = b.fromUser ? scheme.onPrimary : scheme.onSurface;
    return Row(
      mainAxisAlignment:
          b.fromUser ? MainAxisAlignment.end : MainAxisAlignment.start,
      children: [
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 320),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 3),
            child: Material(
              color: bubbleColor,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(16),
                topRight: const Radius.circular(16),
                bottomLeft: Radius.circular(b.fromUser ? 16 : 4),
                bottomRight: Radius.circular(b.fromUser ? 4 : 16),
              ),
              child: Padding(
                padding: const EdgeInsets.only(
                    left: 12, right: 4, top: 8, bottom: 8),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Flexible(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(b.et,
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: onBubble)),
                          if (b.tr.isNotEmpty && !vm.hideTr)
                            Text(b.tr,
                                style: TextStyle(
                                    fontSize: 12.5,
                                    color: onBubble.withOpacity(0.75))),
                        ],
                      ),
                    ),
                    const SizedBox(width: 4),
                    SpeakButton(
                      onPressed: () =>
                          vm.speak(b.et, speaker: b.fromUser ? 'B' : 'A'),
                      tint: b.fromUser ? scheme.onPrimary : scheme.primary,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _typingWidget(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Material(
          color: scheme.surfaceContainerHighest,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(16),
            topRight: Radius.circular(16),
            bottomLeft: Radius.circular(4),
            bottomRight: Radius.circular(16),
          ),
          child: Padding(
            padding:
                const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            child: Text('печатает •••',
                style: TextStyle(
                    fontSize: 14, color: scheme.onSurface.withOpacity(0.6))),
          ),
        ),
      ],
    );
  }
}

// ============================ ТЕСТ ============================

class _Question {
  final String prompt; // что показываем
  final String correct; // правильный вариант
  final List<String> options;
  final String wordEt; // эстонское слово (для отметки «выучено»)
  final bool etToTr;
  _Question(this.prompt, this.correct, this.options, this.wordEt, this.etToTr);
}

class TestTab extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const TestTab({super.key, required this.vm, required this.lesson});

  @override
  State<TestTab> createState() => _TestTabState();
}

class _TestTabState extends State<TestTab> {
  List<_Question> questions = [];
  int current = 0;
  int correct = 0;
  int? picked;
  bool finished = false;
  final Set<String> correctWords = {};
  final rnd = Random();

  @override
  void initState() {
    super.initState();
    _generate();
  }

  void _generate() {
    final words = widget.lesson.words;
    final pool = [...words]..shuffle(rnd);
    final count = min(10, pool.length);
    questions = [];
    for (var i = 0; i < count; i++) {
      final w = pool[i];
      final etToTr = rnd.nextBool();
      final wrong = ([...words]..remove(w)..shuffle(rnd)).take(3);
      final options = etToTr
          ? [w.tr, ...wrong.map((x) => x.tr)]
          : [w.et, ...wrong.map((x) => x.et)];
      options.shuffle(rnd);
      questions.add(_Question(
        etToTr ? w.et : w.tr,
        etToTr ? w.tr : w.et,
        options,
        w.et,
        etToTr,
      ));
    }
    current = 0;
    correct = 0;
    picked = null;
    finished = false;
    correctWords.clear();
  }

  void _pick(int i) {
    if (picked != null) return;
    setState(() {
      picked = i;
      final q = questions[current];
      if (q.options[i] == q.correct) {
        correct++;
        correctWords.add(q.wordEt);
      }
    });
  }

  void _next() {
    if (current + 1 >= questions.length) {
      widget.vm.recordTest(
          widget.lesson.id, correct, questions.length, correctWords);
      setState(() => finished = true);
    } else {
      setState(() {
        current++;
        picked = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (questions.length < 4) {
      return const Center(child: Text('В этом уроке мало слов для теста.'));
    }
    if (finished) {
      final pct = (correct * 100) ~/ questions.length;
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(pct >= 80 ? '🏆' : (pct >= 50 ? '👍' : '💪'),
                  style: const TextStyle(fontSize: 64)),
              Text('Результат: $correct из ${questions.length} ($pct%)',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 20,
                      color: scheme.onSurface)),
              const SizedBox(height: 6),
              Text(
                  pct >= 80
                      ? 'Отлично! Урок пройден.'
                      : 'Повторите слова и попробуйте ещё раз.',
                  style: TextStyle(
                      fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
              const SizedBox(height: 20),
              FilledButton(
                onPressed: () => setState(_generate),
                child: const Text('Пройти ещё раз'),
              ),
            ],
          ),
        ),
      );
    }
    final q = questions[current];
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LinearProgressIndicator(value: current / questions.length),
          const SizedBox(height: 14),
          Text('Вопрос ${current + 1} из ${questions.length}',
              style: TextStyle(
                  fontSize: 13, color: scheme.onSurface.withOpacity(0.6))),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: Text(
                  q.etToTr
                      ? 'Как переводится: «${q.prompt}»?'
                      : 'Как по-эстонски: «${q.prompt}»?',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                      color: scheme.onSurface),
                ),
              ),
              if (q.etToTr)
                SpeakButton(onPressed: () => widget.vm.speakWord(q.prompt)),
            ],
          ),
          const SizedBox(height: 14),
          for (var i = 0; i < q.options.length; i++)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Builder(builder: (context) {
                Color bg = scheme.surface;
                Color border = scheme.onSurface.withOpacity(0.15);
                if (picked != null) {
                  if (q.options[i] == q.correct) {
                    bg = scheme.primaryContainer;
                    border = scheme.primary;
                  } else if (picked == i) {
                    bg = scheme.errorContainer;
                    border = scheme.error;
                  }
                }
                return Material(
                  color: bg,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => _pick(i),
                    child: Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: border, width: 2),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      padding: const EdgeInsets.all(14),
                      child: Text(q.options[i],
                          style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color: scheme.onSurface)),
                    ),
                  ),
                );
              }),
            ),
          const Spacer(),
          FilledButton(
            onPressed: picked != null ? _next : null,
            style: FilledButton.styleFrom(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14))),
            child: Text(
                current + 1 >= questions.length ? 'Завершить' : 'Дальше',
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
