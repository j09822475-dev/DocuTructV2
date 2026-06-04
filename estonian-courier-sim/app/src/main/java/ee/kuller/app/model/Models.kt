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

/** Сценарий доставки — определяет, как строится средняя часть диалога. */
enum class Scenario {
    FACE_DOOR,       // клиент спускается / встречает у двери
    LIFT_BROKEN,     // лифт сломан, спускается сам
    GATE_CODE,       // заперт домофон, нужен код
    DIRECTIONS,      // клиент объясняет, как к нему пройти/проехать
    LEAVE_DOOR,      // оставить под дверью
    CASH,            // оплата наличными
    WRONG_ADDRESS,   // неверный адрес — уточнить
    NOT_HOME,        // клиента нет дома
    LATE,            // курьер опоздал (пробка) — извиниться
    OFFICE,          // доставка в офис — кабинет/этаж
    CANCELLED,       // клиент отменяет заказ
}

/** One answer option the courier can pick. */
data class Choice(
    val et: String,
    val ru: String,
    val correct: Boolean
)

/**
 * Реплика-ход диалога доставки. Диалог показывается в чате ПО ОДНОЙ реплике:
 *  - [Say] — реплику говорит персонаж (показывается и озвучивается автоматически,
 *            с индикатором «печатает…» перед ней);
 *  - [Ask] — ход курьера: он выбирает правильный эстонский вариант.
 */
sealed interface Turn {
    data class Say(
        val speaker: Speaker,
        val et: String,
        val ru: String
    ) : Turn

    data class Ask(
        val promptRu: String,
        val courierAsks: Boolean,
        val choices: List<Choice>,
        val teachWordIds: List<String> = emptyList()
    ) : Turn
}

/**
 * Заказ хранит ФАКТЫ; сами реплики собираются генератором [ee.kuller.app.data.DialogueFactory]
 * на лету из блоков-заготовок, поэтому диалог каждый раз немного разный,
 * но структура (получение → дорога/поиск клиента → передача) сохраняется.
 */
data class Order(
    val id: String,
    val restaurant: String,
    val customer: String,
    val address: String,
    val distanceKm: Double,
    val payout: Double,
    val itemsEt: String,        // что в заказе (по-эстонски)
    val itemsRu: String,        // перевод
    val confirmEt: String,      // как курьер подтверждает заказ (по-эстонски)
    val confirmRu: String,      // перевод подтверждения
    val itemTeach: List<String>,// слова, которым учит состав заказа
    val scenario: Scenario
)
