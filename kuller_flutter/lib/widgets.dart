import 'package:flutter/material.dart';

/// Маленькая «таблетка» со статистикой курьера.
class StatPill extends StatelessWidget {
  final String emoji;
  final String value;
  final String label;
  const StatPill(this.emoji, this.value, this.label, {super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      color: scheme.surface,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 6),
        child: Column(
          children: [
            Text(emoji, style: const TextStyle(fontSize: 20)),
            FittedBox(
              child: Text(value,
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 17,
                      color: scheme.onSurface)),
            ),
            Text(label,
                style: TextStyle(
                    fontSize: 11, color: scheme.onSurface.withOpacity(0.6))),
          ],
        ),
      ),
    );
  }
}

/// Кнопка-динамик для озвучки эстонского слова.
class SpeakButton extends StatelessWidget {
  final VoidCallback onPressed;
  final Color? tint;
  const SpeakButton({super.key, required this.onPressed, this.tint});

  @override
  Widget build(BuildContext context) {
    final color = tint ?? Theme.of(context).colorScheme.primary;
    return Container(
      width: 40,
      height: 40,
      decoration:
          BoxDecoration(color: color.withOpacity(0.12), shape: BoxShape.circle),
      child: IconButton(
        padding: EdgeInsets.zero,
        icon: Icon(Icons.volume_up, color: color, size: 22),
        tooltip: 'Озвучить',
        onPressed: onPressed,
      ),
    );
  }
}

/// Заголовок секции.
class SectionTitle extends StatelessWidget {
  final String text;
  const SectionTitle(this.text, {super.key});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Text(text,
            style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 18,
                color: Theme.of(context).colorScheme.onSurface)),
      );
}

/// Строка «эстонский — русский» с озвучкой.
class WordRow extends StatelessWidget {
  final String et;
  final String ru;
  final String example;
  final VoidCallback onSpeak;
  const WordRow({
    super.key,
    required this.et,
    required this.ru,
    required this.example,
    required this.onSpeak,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      elevation: 1,
      margin: const EdgeInsets.symmetric(vertical: 4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      color: scheme.surface,
      child: Padding(
        padding: const EdgeInsets.all(12).copyWith(left: 16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(et,
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 17,
                          color: scheme.onSurface)),
                  Text(ru,
                      style: TextStyle(
                          fontSize: 14,
                          color: scheme.onSurface.withOpacity(0.7))),
                  if (example.isNotEmpty)
                    Text('„$example“',
                        style: TextStyle(fontSize: 12, color: scheme.primary)),
                ],
              ),
            ),
            const SizedBox(width: 8),
            SpeakButton(onPressed: onSpeak),
          ],
        ),
      ),
    );
  }
}
