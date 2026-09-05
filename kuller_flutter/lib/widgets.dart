import 'package:flutter/material.dart';

/// Маленькая «таблетка» со статистикой.
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

/// Кнопка-динамик для озвучки эстонского текста.
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
