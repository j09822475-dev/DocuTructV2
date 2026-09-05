import 'package:flutter/material.dart';

import '../content.dart';
import '../game_vm.dart';
import '../widgets.dart';

/// Разговорник: готовые фразы курьера по темам, с озвучкой.
class PhrasebookScreen extends StatelessWidget {
  final GameViewModel vm;
  const PhrasebookScreen({super.key, required this.vm});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    // В разговорнике сначала самые нужные курьеру темы
    const order = ['phrase', 'greet', 'money', 'foodq', 'dir', 'num', 'food', 'drink'];
    final cats = [
      for (final id in order)
        Content.categories.firstWhere((c) => c.id == id),
    ];

    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16, bottom: 4),
          child: Text('Vestmik · Разговорник',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 24,
                  color: scheme.onSurface)),
        ),
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text('Нажми 🔊, чтобы услышать произношение',
              style: TextStyle(
                  fontSize: 13, color: scheme.onSurface.withOpacity(0.7))),
        ),
        for (final cat in cats) ...[
          SectionTitle('${cat.emoji}  ${cat.titleEt} · ${cat.titleRu}'),
          for (final word in cat.words)
            WordRow(
              et: word.et,
              ru: word.ru,
              example: word.example,
              onSpeak: () =>
                  vm.tts.speak(word.example.isNotEmpty ? word.example : word.et),
            ),
        ],
        const SizedBox(height: 16),
      ],
    );
  }
}
