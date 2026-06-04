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

/** One interactive step of a delivery: an NPC says a line in Estonian,
 *  the learner must choose the right Estonian reply / meaning. */
data class DialogueStep(
    val speaker: Speaker,
    val npcEt: String,          // что говорит персонаж (по-эстонски)
    val npcRu: String,          // перевод реплики
    val questionRu: String,     // задание для ученика (по-русски)
    val choices: List<Choice>,
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
