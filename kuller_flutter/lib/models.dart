/// Модели данных Kuller — уроки эстонского языка.

/// Предложение: эстонский + перевод (перевод может отсутствовать).
class Sent {
  final String et;
  final String tr;
  const Sent(this.et, [this.tr = '']);
}

/// Карточка слова/фразы.
class WordCard {
  final String et;
  final String tr;
  final String emoji;
  final String example;
  const WordCard(this.et, this.tr, {this.emoji = '', this.example = ''});
}

/// Вопрос с ответом (для текстов и тестов на понимание).
class QA {
  final String q;
  final String a;
  const QA(this.q, this.a);
}

/// Учебный текст урока.
class LessonText {
  final String title;
  final List<Sent> paras;
  final List<QA> questions;
  const LessonText(this.title, this.paras, {this.questions = const []});
}

/// Реплика диалога.
class DialogueLine {
  final String speaker; // 'A' / 'B' / имя
  final String et;
  final String tr;
  const DialogueLine(this.speaker, this.et, [this.tr = '']);
}

class Dialogue {
  final String title;
  final List<DialogueLine> lines;
  const Dialogue(this.title, this.lines);
}

/// Урок: слова + тексты + диалоги (+ тест генерируется из слов).
class Lesson {
  final String id;
  final String emoji;
  final String title; // название по-русски
  final String subtitle; // краткое описание
  final List<WordCard> words;
  final List<LessonText> texts;
  final List<Dialogue> dialogues;
  const Lesson({
    required this.id,
    required this.emoji,
    required this.title,
    this.subtitle = '',
    this.words = const [],
    this.texts = const [],
    this.dialogues = const [],
  });
}
