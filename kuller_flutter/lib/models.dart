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

/// Ход интерактивного диалога: либо реплика собеседника, либо выбор ученика.
sealed class DTurn {
  const DTurn();
}

/// Реплика собеседника (появляется сама, с «печатает…» и озвучкой).
class DSay extends DTurn {
  final String et;
  final String tr;
  const DSay(this.et, [this.tr = '']);
}

/// Точка выбора: ученик выбирает свою реплику из вариантов.
/// После выбора проигрывается ветка [DChoice.followUp], затем диалог идёт дальше.
class DAsk extends DTurn {
  final String prompt; // подсказка по-русски: что нужно сказать
  final List<DChoice> options;
  const DAsk(this.prompt, this.options);
}

class DChoice {
  final String et;
  final String tr;
  final bool correct;
  final List<DSay> followUp; // реакция собеседника на этот выбор
  const DChoice(this.et, this.tr,
      {this.correct = true, this.followUp = const []});
}

/// Интерактивный диалог урока.
class Dialogue {
  final String title;
  final List<DTurn> turns;
  const Dialogue(this.title, this.turns);
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
