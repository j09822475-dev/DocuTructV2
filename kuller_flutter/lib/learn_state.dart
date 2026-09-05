import 'package:shared_preferences/shared_preferences.dart';

/// Прогресс обучения: лучшие результаты тестов и выученные слова.
class LearnState {
  final Map<String, int> bestScores; // lessonId -> лучший результат теста, %
  final Set<String> learnedWords; // эстонские слова, отвеченные верно в тестах
  final int testsTaken;

  const LearnState({
    this.bestScores = const {},
    this.learnedWords = const {},
    this.testsTaken = 0,
  });

  LearnState copyWith({
    Map<String, int>? bestScores,
    Set<String>? learnedWords,
    int? testsTaken,
  }) =>
      LearnState(
        bestScores: bestScores ?? this.bestScores,
        learnedWords: learnedWords ?? this.learnedWords,
        testsTaken: testsTaken ?? this.testsTaken,
      );
}

class LearnRepository {
  final SharedPreferences _prefs;
  LearnRepository(this._prefs);

  static Future<LearnRepository> create() async =>
      LearnRepository(await SharedPreferences.getInstance());

  LearnState load() {
    final scoresStr = _prefs.getString('scores') ?? '';
    final scores = <String, int>{};
    for (final e in scoresStr.split(';')) {
      final p = e.split(':');
      if (p.length == 2) scores[p[0]] = int.tryParse(p[1]) ?? 0;
    }
    return LearnState(
      bestScores: scores,
      learnedWords: (_prefs.getStringList('learnedWords') ?? []).toSet(),
      testsTaken: _prefs.getInt('testsTaken') ?? 0,
    );
  }

  Future<void> save(LearnState s) async {
    await _prefs.setString('scores',
        s.bestScores.entries.map((e) => '${e.key}:${e.value}').join(';'));
    await _prefs.setStringList('learnedWords', s.learnedWords.toList());
    await _prefs.setInt('testsTaken', s.testsTaken);
  }

  Future<void> clear() async => _prefs.clear();

  bool loadHideTr() => _prefs.getBool('hideTr') ?? false;
  Future<void> saveHideTr(bool v) async => _prefs.setBool('hideTr', v);
}
