package ee.kuller.app.model

/** A single vocabulary item: Estonian word + Russian translation. */
data class WordEntry(
    val id: String,
    val et: String,
    val ru: String,
    val example: String = ""
)

/** Thematic group of words. */
data class Category(
    val id: String,
    val titleEt: String,
    val titleRu: String,
    val emoji: String,
    val words: List<WordEntry>
)

/** Сценарий доставки. */
enum class Scenario {
    FACE_DOOR, LIFT_BROKEN, GATE_CODE, DIRECTIONS, LEAVE_DOOR, CASH,
    WRONG_ADDRESS, NOT_HOME, LATE, OFFICE, CANCELLED,
    COMPLAINT, BREAKDOWN, SPOILED, INTERCOM,
    ANGRY_CLIENT, REFUSE_PAY, DRUNK_CLIENT, DOG_YARD, WRONG_ITEMS,
}

/** Отдельные чаты-собеседники. */
enum class Thread { RESTORAN, KLIENT, TUGI }

/**
 * Вариант ответа курьера. Любой вариант РАБОЧИЙ: после выбора разворачивается
 * его ветка [followUp], затем диалог сходится к завершению текущей фазы.
 *  - [correct] — рекомендуемый/вежливый вариант (даёт XP);
 *  - [ratingDelta] — влияние на рейтинг.
 */
data class Choice(
    val et: String,
    val ru: String,
    val correct: Boolean = true,
    val followUp: List<Turn> = emptyList(),
    val ratingDelta: Double = 0.0,
)

/** Реплика-ход диалога. У каждого хода есть чат [thread]. */
sealed interface Turn {
    val thread: Thread

    data class Say(
        override val thread: Thread,
        val et: String,
        val ru: String
    ) : Turn

    /** Вопрос-развилка: каждый вариант ведёт в свою ветку. */
    data class Ask(
        override val thread: Thread,
        val promptRu: String,
        val courier: Boolean,
        val choices: List<Choice>,
        val teachWordIds: List<String> = emptyList()
    ) : Turn
}

/** Что происходит после завершения фазы (реплики в одном чате закончились). */
sealed interface Nav {
    /** Доставка завершена. */
    data object End : Nav

    /** Курьер сам выбирает, что делать дальше (какой чат открыть). */
    data class Choose(val promptRu: String, val options: List<NavOption>) : Nav
}

data class NavOption(val labelRu: String, val phase: String)

/** Фаза диалога — разговор в ОДНОМ чате, затем навигация [nav]. */
data class Phase(
    val id: String,
    val thread: Thread,
    val turns: List<Turn>,
    val nav: Nav
)

/** Готовый диалог доставки: стартовая фаза + все фазы по id. */
data class Delivery(
    val start: String,
    val phases: Map<String, Phase>
)

/** Заказ хранит факты; диалог собирает DialogueFactory. */
data class Order(
    val id: String,
    val restaurant: String,
    val customer: String,
    val address: String,
    val distanceKm: Double,
    val payout: Double,
    val itemsEt: String,
    val itemsRu: String,
    val confirmEt: String,
    val confirmRu: String,
    val itemTeach: List<String>,
    val scenario: Scenario
)
