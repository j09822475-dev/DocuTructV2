import 'package:flutter/material.dart';

import '../data/lessons.dart';
import '../learn_vm.dart';
import '../widgets.dart';

class ProfileScreen extends StatefulWidget {
  final LearnViewModel vm;
  const ProfileScreen({super.key, required this.vm});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool confirmReset = false;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    final s = vm.state;
    final totalWords =
        allLessons.fold<int>(0, (sum, l) => sum + l.words.length);
    final passed =
        allLessons.where((l) => vm.bestScore(l.id) >= 80).length;
    final started =
        allLessons.where((l) => vm.bestScore(l.id) > 0).length;
    final avg = started == 0
        ? 0
        : allLessons.map((l) => vm.bestScore(l.id)).reduce((a, b) => a + b) ~/
            allLessons.length;

    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16, bottom: 8),
          child: Text('Profiil · Прогресс',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 24,
                  color: scheme.onSurface)),
        ),
        Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          color: scheme.primaryContainer,
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Пройдено уроков: $passed из ${allLessons.length}',
                    style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 20,
                        color: scheme.onPrimaryContainer)),
                Text('Урок считается пройденным при результате теста ≥ 80%',
                    style: TextStyle(
                        fontSize: 12,
                        color: scheme.onPrimaryContainer.withOpacity(0.8))),
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: LinearProgressIndicator(
                      value: allLessons.isEmpty
                          ? 0
                          : passed / allLessons.length),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
                child: StatPill(
                    '📚', '${s.learnedWords.length}/$totalWords', 'Слова')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('📝', '${s.testsTaken}', 'Тестов')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('📈', '$avg%', 'Средний балл')),
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(top: 20, bottom: 8),
          child: Text('Уроки',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                  color: scheme.onSurface)),
        ),
        for (var i = 0; i < allLessons.length; i++)
          Builder(builder: (context) {
            final lesson = allLessons[i];
            final score = vm.bestScore(lesson.id);
            return Card(
              elevation: 0,
              margin: const EdgeInsets.symmetric(vertical: 3),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14)),
              color: scheme.surface,
              child: Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                child: Row(
                  children: [
                    Text(lesson.emoji, style: const TextStyle(fontSize: 18)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text('${i + 1}. ${lesson.title}',
                          style: TextStyle(
                              fontSize: 14, color: scheme.onSurface)),
                    ),
                    Text(
                        score >= 80
                            ? '🏆 $score%'
                            : (score > 0 ? '$score%' : '—'),
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                            color: score >= 80
                                ? scheme.primary
                                : scheme.onSurface.withOpacity(0.6))),
                  ],
                ),
              ),
            );
          }),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 20),
          child: OutlinedButton(
            onPressed: () {
              if (confirmReset) {
                vm.resetProgress();
                setState(() => confirmReset = false);
              } else {
                setState(() => confirmReset = true);
              }
            },
            child: Text(confirmReset
                ? 'Точно сбросить весь прогресс?'
                : 'Сбросить прогресс'),
          ),
        ),
      ],
    );
  }
}
