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

/** One answer option inside a dialogue step. */
data class Choice(
    val et: String,
    val ru: String,
    val correct: Boolean
)

/** One interactive step of a delivery — двусторонний диалог.
 *
 *  Сценарий шага:
 *   1. (опц.) персонаж говорит реплику [npcEt] (если пусто — реплику начинает курьер);
 *   2. ученик-курьер выбирает правильный эстонский вариант из [choices]
 *      (вопрос или ответ — зависит от [courierAsks]);
 *   3. (опц.) персонаж отвечает [npcReplyEt] — так получается живой обмен фразами. */
data class DialogueStep(
    val speaker: Speaker,
    val npcEt: String,                  // что говорит персонаж первым (может быть пусто)
    val npcRu: String,                  // перевод реплики персонажа
    val questionRu: String,             // задание для ученика (по-русски)
    val choices: List<Choice>,
    val courierAsks: Boolean = false,   // true → курьер сам задаёт вопрос
    val npcReplyEt: String = "",        // ответ персонажа после верного выбора
    val npcReplyRu: String = "",        // перевод ответа персонажа
    val teachWordIds: List<String> = emptyList()
)

/** A delivery order = a mini-lesson framed as a Bolt/Wolt courier job. */
data class Order(
    val id: String,
    val restaurant: String,
    val customer: String,
    val address: String,       // эстонский адрес
    val distanceKm: Double,
    val payout: Double,        // € за доставку
    val itemsEt: String,       // что в заказе (по-эстонски)
    val itemsRu: String,       // перевод
    val steps: List<DialogueStep>
)
