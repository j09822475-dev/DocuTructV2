/// Модели данных Kuller (порт с Kotlin-версии).

/// Слово словаря: эстонское слово + русский перевод.
class WordEntry {
  final String id;
  final String et;
  final String ru;
  final String example;
  const WordEntry(this.id, this.et, this.ru, [this.example = '']);
}

/// Тематическая группа слов.
class Category {
  final String id;
  final String titleEt;
  final String titleRu;
  final String emoji;
  final List<WordEntry> words;
  const Category({
    required this.id,
    required this.titleEt,
    required this.titleRu,
    required this.emoji,
    required this.words,
  });
}

/// Сценарий доставки.
enum Scenario {
  faceDoor,
  liftBroken,
  gateCode,
  directions,
  leaveDoor,
  cash,
  wrongAddress,
  notHome,
  lateOrder,
  office,
  cancelled,
  complaint,
  breakdown,
  spoiled,
  intercom,
  angryClient,
  refusePay,
  drunkClient,
  dogYard,
  wrongItems,
}

/// Отдельные чаты-собеседники.
enum ChatThread { restoran, klient, tugi }

/// Вариант ответа курьера. Любой вариант РАБОЧИЙ: после выбора разворачивается
/// его ветка [followUp], затем диалог сходится к завершению текущей фазы.
///  - [correct] — рекомендуемый/вежливый вариант (даёт XP);
///  - [ratingDelta] — влияние на рейтинг.
class Choice {
  final String et;
  final String ru;
  final bool correct;
  final List<Turn> followUp;
  final double ratingDelta;
  const Choice(
    this.et,
    this.ru, {
    this.correct = true,
    this.followUp = const [],
    this.ratingDelta = 0.0,
  });
}

/// Реплика-ход диалога. У каждого хода есть чат [thread].
sealed class Turn {
  ChatThread get thread;
}

class Say extends Turn {
  @override
  final ChatThread thread;
  final String et;
  final String ru;
  Say(this.thread, this.et, this.ru);
}

/// Вопрос-развилка: каждый вариант ведёт в свою ветку.
class Ask extends Turn {
  @override
  final ChatThread thread;
  final String promptRu;
  final bool courier;
  final List<Choice> choices;
  final List<String> teachWordIds;
  Ask(this.thread, this.promptRu, this.courier, this.choices,
      [this.teachWordIds = const []]);

  Ask withChoices(List<Choice> newChoices) =>
      Ask(thread, promptRu, courier, newChoices, teachWordIds);
}

/// Что происходит после завершения фазы (реплики в одном чате закончились).
sealed class Nav {
  const Nav();
}

/// Доставка завершена.
class NavEnd extends Nav {
  const NavEnd();
}

/// Курьер сам выбирает, что делать дальше (какой чат открыть).
class NavChoose extends Nav {
  final String promptRu;
  final List<NavOption> options;
  const NavChoose(this.promptRu, this.options);
}

class NavOption {
  final String labelRu;
  final String phase;
  const NavOption(this.labelRu, this.phase);
}

/// Фаза диалога — разговор в ОДНОМ чате, затем навигация [nav].
class Phase {
  final String id;
  final ChatThread thread;
  final List<Turn> turns;
  final Nav nav;
  Phase(this.id, this.thread, this.turns, this.nav);
}

/// Готовый диалог доставки: стартовая фаза + все фазы по id.
class Delivery {
  final String start;
  final Map<String, Phase> phases;
  Delivery(this.start, this.phases);
}

/// Заказ хранит факты; диалог собирает DialogueFactory.
class Order {
  final String id;
  final String restaurant;
  final String customer;
  final String address;
  final double distanceKm;
  final double payout;
  final String itemsEt;
  final String itemsRu;
  final String confirmEt;
  final String confirmRu;
  final List<String> itemTeach;
  final Scenario scenario;
  const Order({
    required this.id,
    required this.restaurant,
    required this.customer,
    required this.address,
    required this.distanceKm,
    required this.payout,
    required this.itemsEt,
    required this.itemsRu,
    required this.confirmEt,
    required this.confirmRu,
    required this.itemTeach,
    required this.scenario,
  });
}

String threadLabel(ChatThread thread) => switch (thread) {
      ChatThread.restoran => '🏪 Ресторан',
      ChatThread.klient => '🙋 Клиент',
      ChatThread.tugi => '🎧 Поддержка',
    };

const threadOrder = [ChatThread.restoran, ChatThread.klient, ChatThread.tugi];

/// Сообщение в чате (для ленты).
class ChatMsg {
  final ChatThread thread;
  final bool fromCourier;
  final String et;
  final String ru;
  final String label;
  ChatMsg(this.thread, this.fromCourier, this.et, this.ru, this.label);
}
