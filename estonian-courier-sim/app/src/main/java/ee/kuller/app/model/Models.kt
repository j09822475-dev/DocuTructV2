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

/** Сценарий доставки — определяет среднюю часть диалога. */
enum class Scenario {
    FACE_DOOR, LIFT_BROKEN, GATE_CODE, DIRECTIONS, LEAVE_DOOR, CASH,
    WRONG_ADDRESS, NOT_HOME, LATE, OFFICE, CANCELLED,
    COMPLAINT,    // клиент жалуется (холодная еда)
    BREAKDOWN,    // поломка транспорта в пути
    SPOILED,      // повреждённая упаковка / еда вытекла
}

/** Отдельные чаты-собеседники (как в реальном приложении курьера). */
enum class Thread { RESTORAN, KLIENT, TUGI }

/**
 * Вариант ответа курьера.
 *  - [correct] — рекомендуемый/вежливый вариант (даёт XP);
 *  - [followUp] — ветка диалога, которая разворачивается после выбора;
 *  - [ratingDelta] — как выбор влияет на рейтинг курьера.
 */
data class Choice(
    val et: String,
    val ru: String,
    val correct: Boolean = true,
    val followUp: List<Turn> = emptyList(),
    val ratingDelta: Double = 0.0,
)

/** Реплика-ход диалога. У каждого хода есть [thread] — в каком чате он происходит. */
sealed interface Turn {
    val thread: Thread

    data class Say(
        override val thread: Thread,
        val et: String,
        val ru: String
    ) : Turn

    data class Ask(
        override val thread: Thread,
        val promptRu: String,
        val courier: Boolean,        // курьер сам спрашивает (для подписи)
        val branching: Boolean,      // true → любой вариант ведёт в свою ветку (без «попробуй ещё»)
        val choices: List<Choice>,
        val teachWordIds: List<String> = emptyList()
    ) : Turn
}

/** Заказ хранит факты; реплики собирает DialogueFactory. */
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
