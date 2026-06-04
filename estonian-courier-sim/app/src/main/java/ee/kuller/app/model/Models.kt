package ee.kuller.app.model

/** A single vocabulary item: Estonian word + Russian translation. */
data class WordEntry(
    val id: String,
    val et: String,        // eesti keel
    val ru: String,        // русский
    val example: String = "" // näide / пример (Estonian sentence)
)

/** Thematic group of words (Toit, Tervitused, Numbrid ...). */
data class Category(
    val id: String,
    val titleEt: String,
    val titleRu: String,
    val emoji: String,
    val words: List<WordEntry>
)

enum class Speaker { RESTORAN, KLIENT, NARRATOR }

/** One answer option the courier can pick. */
data class Choice(
    val et: String,
    val ru: String,
    val correct: Boolean
)

/**
 * Реплика-ход диалога доставки. Диалог — это плоская последовательность ходов,
 * которые показываются в чате ПО ОДНОМУ:
 *  - [Say]  — реплику говорит персонаж (ресторан / клиент / навигатор);
 *             показывается и озвучивается автоматически;
 *  - [Ask]  — ход курьера: он выбирает правильный эстонский вариант,
 *             выбранная фраза становится сообщением курьера.
 */
sealed interface Turn {
    data class Say(
        val speaker: Speaker,
        val et: String,
        val ru: String
    ) : Turn

    data class Ask(
        val promptRu: String,        // задание для ученика
        val courierAsks: Boolean,    // курьер сам задаёт вопрос (для подписи)
        val choices: List<Choice>,
        val teachWordIds: List<String> = emptyList()
    ) : Turn
}

/** A delivery order = a chat-style mini-lesson framed as a courier job. */
data class Order(
    val id: String,
    val restaurant: String,
    val customer: String,
    val address: String,       // эстонский адрес
    val distanceKm: Double,
    val payout: Double,        // € за доставку
    val itemsEt: String,       // что в заказе (по-эстонски)
    val itemsRu: String,       // перевод
    val turns: List<Turn>
)
