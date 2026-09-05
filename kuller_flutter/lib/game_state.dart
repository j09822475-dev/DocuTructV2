import 'package:shared_preferences/shared_preferences.dart';

/// Постоянный прогресс игрока-курьера.
class GameState {
  final double money; // заработано €
  final int xp; // опыт (очки изучения)
  final int deliveries; // выполнено доставок
  final double rating; // рейтинг курьера
  final int correct; // верных ответов
  final int wrong; // ошибок
  final Set<String> learnedIds; // выученные слова
  final Map<String, int> reps; // сколько раз слово отработано (для скрытия перевода)

  const GameState({
    this.money = 0.0,
    this.xp = 0,
    this.deliveries = 0,
    this.rating = 5.0,
    this.correct = 0,
    this.wrong = 0,
    this.learnedIds = const {},
    this.reps = const {},
  });

  int get level => xp ~/ 100 + 1;
  int get xpInLevel => xp % 100;
  int get accuracy =>
      (correct + wrong) == 0 ? 0 : (correct * 100) ~/ (correct + wrong);

  GameState copyWith({
    double? money,
    int? xp,
    int? deliveries,
    double? rating,
    int? correct,
    int? wrong,
    Set<String>? learnedIds,
    Map<String, int>? reps,
  }) =>
      GameState(
        money: money ?? this.money,
        xp: xp ?? this.xp,
        deliveries: deliveries ?? this.deliveries,
        rating: rating ?? this.rating,
        correct: correct ?? this.correct,
        wrong: wrong ?? this.wrong,
        learnedIds: learnedIds ?? this.learnedIds,
        reps: reps ?? this.reps,
      );
}

/// Простое хранилище прогресса в SharedPreferences.
class GameRepository {
  final SharedPreferences _prefs;
  GameRepository(this._prefs);

  static Future<GameRepository> create() async =>
      GameRepository(await SharedPreferences.getInstance());

  GameState load() {
    final repsStr = _prefs.getString('reps') ?? '';
    final reps = <String, int>{};
    for (final e in repsStr.split(';')) {
      final p = e.split(':');
      if (p.length == 2) reps[p[0]] = int.tryParse(p[1]) ?? 0;
    }
    return GameState(
      money: _prefs.getDouble('money') ?? 0.0,
      xp: _prefs.getInt('xp') ?? 0,
      deliveries: _prefs.getInt('deliveries') ?? 0,
      rating: _prefs.getDouble('rating') ?? 5.0,
      correct: _prefs.getInt('correct') ?? 0,
      wrong: _prefs.getInt('wrong') ?? 0,
      learnedIds: (_prefs.getStringList('learned') ?? []).toSet(),
      reps: reps,
    );
  }

  Future<void> save(GameState s) async {
    await _prefs.setDouble('money', s.money);
    await _prefs.setInt('xp', s.xp);
    await _prefs.setInt('deliveries', s.deliveries);
    await _prefs.setDouble('rating', s.rating);
    await _prefs.setInt('correct', s.correct);
    await _prefs.setInt('wrong', s.wrong);
    await _prefs.setStringList('learned', s.learnedIds.toList());
    await _prefs.setString(
        'reps', s.reps.entries.map((e) => '${e.key}:${e.value}').join(';'));
  }

  Future<void> clear() async => _prefs.clear();

  bool loadHideRu() => _prefs.getBool('hideRu') ?? false;
  Future<void> saveHideRu(bool v) async => _prefs.setBool('hideRu', v);
}
