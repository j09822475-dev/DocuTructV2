import 'package:flutter_tts/flutter_tts.dart';

/// Озвучка эстонского текста (системный TTS).
/// Тон голоса задаётся per-фраза: собеседник — ниже, ученик — выше.
class Speaker {
  final FlutterTts _tts = FlutterTts();
  bool _ready = false;

  Future<void> _init() async {
    if (_ready) return;
    _ready = true;
    try {
      await _tts.setLanguage('et-EE');
      await _tts.setSpeechRate(0.5);
      // speak() завершается только когда фраза ДОГОВОРЕНА —
      // это позволяет диалогу ждать конца озвучки.
      await _tts.awaitSpeakCompletion(true);
    } catch (_) {
      // Нет эстонского голоса — говорим голосом по умолчанию.
    }
  }

  /// Озвучить, не дожидаясь конца (кнопки 🔊, карточки).
  void speak(String text, {double pitch = 1.0}) {
    speakAwait(text, pitch: pitch);
  }

  /// Озвучить и дождаться, пока фраза прозвучит целиком
  /// (используется в диалогах, чтобы реплики не перебивали друг друга).
  Future<void> speakAwait(String text, {double pitch = 1.0}) async {
    await _init();
    try {
      await _tts.setPitch(pitch);
      // Таймаут-страховка: если TTS-движок не отчитался о завершении,
      // не подвешиваем диалог навсегда.
      await _tts
          .speak(text)
          .timeout(Duration(milliseconds: 1500 + text.length * 120));
    } catch (_) {}
  }

  void shutdown() {
    try {
      _tts.stop();
    } catch (_) {}
  }
}
