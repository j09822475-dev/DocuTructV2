import 'package:flutter_tts/flutter_tts.dart';

/// Озвучка (системный TTS). Умеет говорить по-эстонски и по-украински
/// (для прослушивания переводов).
class Speaker {
  final FlutterTts _tts = FlutterTts();
  bool _ready = false;
  String _lang = '';

  Future<void> _init() async {
    if (_ready) return;
    _ready = true;
    try {
      await _tts.setSpeechRate(0.5);
      // speak() завершается только когда фраза ДОГОВОРЕНА —
      // это позволяет диалогу/читалке ждать конца озвучки.
      await _tts.awaitSpeakCompletion(true);
    } catch (_) {}
    await _setLang('et-EE');
  }

  Future<void> _setLang(String lang) async {
    if (_lang == lang) return;
    _lang = lang;
    try {
      await _tts.setLanguage(lang);
    } catch (_) {
      // Нет такого голоса — останется голос по умолчанию.
    }
  }

  /// Озвучить, не дожидаясь конца (кнопки 🔊, карточки).
  void speak(String text, {double pitch = 1.0, String lang = 'et-EE'}) {
    speakAwait(text, pitch: pitch, lang: lang);
  }

  /// Озвучить и дождаться, пока фраза прозвучит целиком.
  Future<void> speakAwait(String text,
      {double pitch = 1.0, String lang = 'et-EE'}) async {
    await _init();
    await _setLang(lang);
    try {
      await _tts.setPitch(pitch);
      // Таймаут-страховка: если TTS-движок не отчитался о завершении,
      // не подвешиваем экран навсегда.
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
