import 'package:flutter/material.dart';

import '../content.dart';
import '../game_vm.dart';
import '../widgets.dart';

class ProfileScreen extends StatefulWidget {
  final GameViewModel vm;
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
    final totalWords = Content.allWords.length;

    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16, bottom: 8),
          child: Text('Profiil · Профиль',
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
                Text('Курьер уровня ${s.level}',
                    style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 22,
                        color: scheme.onPrimaryContainer)),
                Text('${s.xpInLevel}/100 XP до следующего уровня',
                    style: TextStyle(
                        fontSize: 13,
                        color: scheme.onPrimaryContainer.withOpacity(0.8))),
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: LinearProgressIndicator(value: s.xpInLevel / 100),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(child: StatPill('💶', vm.moneyStr(), 'Заработок')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('📦', '${s.deliveries}', 'Доставки')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('⭐', vm.ratingStr(), 'Рейтинг')),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(child: StatPill('🎯', '${s.accuracy}%', 'Точность')),
            const SizedBox(width: 10),
            Expanded(
                child:
                    StatPill('📚', '${s.learnedIds.length}/$totalWords', 'Слова')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('✅', '${s.correct}', 'Ответы')),
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(top: 20, bottom: 8),
          child: Text('Прогресс по темам',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                  color: scheme.onSurface)),
        ),
        for (final cat in Content.categories)
          Builder(builder: (context) {
            final learned = vm.learnedInCategory(cat.id);
            return Card(
              margin: const EdgeInsets.symmetric(vertical: 4),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16)),
              color: scheme.surface,
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('${cat.emoji} ${cat.titleRu}',
                            style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: scheme.onSurface)),
                        Text('$learned/${cat.words.length}',
                            style: TextStyle(
                                color: scheme.onSurface.withOpacity(0.7))),
                      ],
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: LinearProgressIndicator(
                          value: cat.words.isEmpty
                              ? 0
                              : learned / cat.words.length),
                    ),
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
