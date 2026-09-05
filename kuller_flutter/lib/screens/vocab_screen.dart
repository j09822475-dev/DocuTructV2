import 'package:flutter/material.dart';

import '../content.dart';
import '../game_vm.dart';
import '../widgets.dart';

class VocabularyScreen extends StatefulWidget {
  final GameViewModel vm;
  const VocabularyScreen({super.key, required this.vm});

  @override
  State<VocabularyScreen> createState() => _VocabularyScreenState();
}

class _VocabularyScreenState extends State<VocabularyScreen> {
  String selected = Content.categories.first.id;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    final category = Content.categories.firstWhere((c) => c.id == selected);
    final learned = vm.learnedInCategory(category.id);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 16, bottom: 4),
            child: Text('Sõnavara · Словарь',
                style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 24,
                    color: scheme.onSurface)),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              children: [
                for (final cat in Content.categories)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      selected: cat.id == selected,
                      onSelected: (_) => setState(() => selected = cat.id),
                      label: Text('${cat.emoji} ${cat.titleRu}'),
                    ),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Text(
                '${category.titleEt} — выучено $learned/${category.words.length}',
                style: TextStyle(
                    fontSize: 13, color: scheme.onSurface.withOpacity(0.7))),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: LinearProgressIndicator(
                value: category.words.isEmpty
                    ? 0
                    : learned / category.words.length),
          ),
          Expanded(
            child: ListView(
              children: [
                for (final word in category.words)
                  WordRow(
                    et: word.et,
                    ru: word.ru,
                    example: word.example,
                    onSpeak: () => vm.tts.speak(word.et),
                  ),
                const SizedBox(height: 16),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
