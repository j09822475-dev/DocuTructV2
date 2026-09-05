import 'package:flutter/foundation.dart';

import 'learn_state.dart';
import 'models.dart';
import 'tts.dart';

class LearnViewModel extends ChangeNotifier {
  final LearnRepository repo;
  final Speaker tts = Speaker();

  LearnState state;

  /// Глобальный переключатель: скрывать ли переводы.
  bool hideTr;

  LearnViewModel(this.repo)
      : state = repo.load(),
        hideTr = repo.loadHideTr();

  void toggleHideTr() {
    hideTr = !hideTr;
    repo.saveHideTr(hideTr);
    notifyListeners();
  }

  double _pitchOf(String speaker) => speaker == 'A' ? 0.9 : 1.2;

  /// Озвучка: собеседник A — базовый тон, B — выше (разные голоса в диалоге).
  void speak(String text, {String speaker = 'A'}) {
    tts.speak(text, pitch: _pitchOf(speaker));
  }

  /// Озвучить и дождаться конца фразы (для последовательных реплик диалога).
  Future<void> speakAwait(String text, {String speaker = 'A'}) =>
      tts.speakAwait(text, pitch: _pitchOf(speaker));

  void speakWord(String text) => tts.speak(text, pitch: 1.0);

  int bestScore(String lessonId) => state.bestScores[lessonId] ?? 0;

  /// Сохранить результат теста урока.
  void recordTest(String lessonId, int correct, int total,
      Iterable<String> correctWords) {
    final pct = total == 0 ? 0 : (correct * 100) ~/ total;
    final scores = Map.of(state.bestScores);
    if (pct > (scores[lessonId] ?? 0)) scores[lessonId] = pct;
    state = state.copyWith(
      bestScores: scores,
      learnedWords: {...state.learnedWords, ...correctWords},
      testsTaken: state.testsTaken + 1,
    );
    repo.save(state);
    notifyListeners();
  }

  int learnedInLesson(Lesson lesson) =>
      lesson.words.where((w) => state.learnedWords.contains(w.et)).length;

  Future<void> resetProgress() async {
    final hide = hideTr;
    await repo.clear();
    await repo.saveHideTr(hide);
    state = const LearnState();
    notifyListeners();
  }
}
