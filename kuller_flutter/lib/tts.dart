import 'package:flutter_tts/flutter_tts.dart';

/// Озвучка эстонского текста (системный TTS).
/// Тон голоса задаётся per-фраза: курьер — базовый, клиент — выше,
/// ресторан — ниже, поддержка — средне.
class Speaker {
  final FlutterTts _tts = FlutterTts();
  bool _ready = false;

  Future<void> _init() async {
    if (_ready) return;
    _ready = true;
    try {
      await _tts.setLanguage('et-EE');
      await _tts.setSpeechRate(0.5);
    } catch (_) {
      // Нет эстонского голоса — говорим голосом по умолчанию.
    }
  }

  Future<void> speak(String text, {double pitch = 1.0}) async {
    await _init();
    try {
      await _tts.setPitch(pitch);
      await _tts.speak(text);
    } catch (_) {}
  }

  void shutdown() {
    try {
      _tts.stop();
    } catch (_) {}
  }
}
