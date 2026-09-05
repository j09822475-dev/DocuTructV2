import 'package:flutter/material.dart';

import '../data/lessons.dart';
import '../learn_vm.dart';
import '../models.dart';
import 'lesson_screen.dart';

class LessonsScreen extends StatelessWidget {
  final LearnViewModel vm;
  const LessonsScreen({super.key, required this.vm});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16),
          child: Text('Eesti keel A2 📚',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 26,
                  color: scheme.onSurface)),
        ),
        Text('Уроки по конспекту курса — слова, тексты, диалоги и тесты',
            style: TextStyle(
                fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
        const SizedBox(height: 12),
        for (var i = 0; i < allLessons.length; i++)
          _LessonCard(
            index: i + 1,
            lesson: allLessons[i],
            vm: vm,
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                  builder: (_) => LessonScreen(vm: vm, lesson: allLessons[i])),
            ),
          ),
        const SizedBox(height: 24),
      ],
    );
  }
}

class _LessonCard extends StatelessWidget {
  final int index;
  final Lesson lesson;
  final LearnViewModel vm;
  final VoidCallback onTap;
  const _LessonCard({
    required this.index,
    required this.lesson,
    required this.vm,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final score = vm.bestScore(lesson.id);
    final learned = vm.learnedInLesson(lesson);
    return Card(
      elevation: 1,
      margin: const EdgeInsets.symmetric(vertical: 5),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      color: scheme.surface,
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: score >= 80
                      ? scheme.primaryContainer
                      : scheme.surfaceContainerHighest,
                  shape: BoxShape.circle,
                ),
                alignment: Alignment.center,
                child: Text(lesson.emoji, style: const TextStyle(fontSize: 22)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Урок $index. ${lesson.title}',
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 15,
                            color: scheme.onSurface)),
                    if (lesson.subtitle.isNotEmpty)
                      Text(lesson.subtitle,
                          style: TextStyle(
                              fontSize: 12,
                              color: scheme.onSurface.withOpacity(0.65))),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        _chip(context, '📇 ${lesson.words.length} слов'),
                        if (lesson.texts.isNotEmpty)
                          _chip(context, '📖 ${lesson.texts.length}'),
                        if (lesson.dialogues.isNotEmpty)
                          _chip(context, '💬 ${lesson.dialogues.length}'),
                        if (learned > 0)
                          _chip(context, '✅ $learned/${lesson.words.length}'),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                children: [
                  if (score > 0)
                    Text('$score%',
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                            color: score >= 80
                                ? scheme.primary
                                : scheme.onSurface.withOpacity(0.7))),
                  if (score >= 80)
                    const Text('🏆', style: TextStyle(fontSize: 16)),
                  if (score == 0)
                    Icon(Icons.chevron_right,
                        color: scheme.onSurface.withOpacity(0.4)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _chip(BuildContext context, String text) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(right: 6),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Text(text,
            style: TextStyle(
                fontSize: 11, color: scheme.onSurface.withOpacity(0.75))),
      ),
    );
  }
}
