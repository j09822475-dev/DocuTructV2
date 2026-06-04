package ee.kuller.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ee.kuller.app.data.Content
import ee.kuller.app.data.DialogueFactory
import ee.kuller.app.data.GameRepository
import ee.kuller.app.data.GameState
import ee.kuller.app.model.Order
import ee.kuller.app.model.Speaker
import ee.kuller.app.model.Turn
import ee.kuller.app.util.Speaker as Tts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** Подпись отправителя сообщения в чате доставки. */
fun npcLabel(speaker: Speaker): String = when (speaker) {
    Speaker.RESTORAN -> "🏪 Ресторан"
    Speaker.KLIENT -> "🙋 Клиент"
    Speaker.NARRATOR -> "🧭 Навигатор"
}

/** Одно сообщение в чате доставки. */
data class ChatMsg(
    val fromCourier: Boolean,   // true → сообщение курьера (справа)
    val et: String,
    val ru: String,
    val label: String
)

/** Активная сессия доставки (один заказ) в виде чата из реплик-ходов. */
data class OrderSession(
    val order: Order,
    val turns: List<Turn>,                // собраны генератором на лету
    val turnIndex: Int = 0,
    val transcript: List<ChatMsg> = emptyList(),
    val awaiting: Boolean = false,        // ждём ответа курьера на текущий Ask
    val atEnd: Boolean = false,           // диалог завершён — показать «Завершить»
    val typing: String? = null,           // подпись «… печатает» (пока готовится реплика NPC)
    val selected: Int? = null,
    val wrongSelected: Set<Int> = emptySet(),
    val correctCount: Int = 0,
    val mistakes: Int = 0,
    val finished: Boolean = false
) {
    val currentAsk: Turn.Ask? get() = turns.getOrNull(turnIndex) as? Turn.Ask
    val totalAsks: Int get() = turns.count { it is Turn.Ask }
    val progress: Float get() = if (totalAsks == 0) 0f else correctCount.toFloat() / totalAsks
}

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository(app)
    private val tts = Tts(app)
    private var revealJob: Job? = null

    var state by mutableStateOf(repo.load())
        private set

    var online by mutableStateOf(false)
        private set

    var session by mutableStateOf<OrderSession?>(null)
        private set

    /** Итог последней доставки — для экрана результата. */
    var lastResult by mutableStateOf<DeliveryResult?>(null)
        private set

    fun toggleOnline() { online = !online }

    /** Озвучить текст вручную (по кнопке 🔊). */
    fun speak(text: String) = tts.speak(text, flush = true)

    fun startOrder(order: Order) {
        revealJob?.cancel()
        // Собираем диалог на лету и перемешиваем варианты (верный не всегда первый).
        val turns = DialogueFactory.build(order).map {
            if (it is Turn.Ask) it.copy(choices = it.choices.shuffled()) else it
        }
        session = OrderSession(order = order, turns = turns)
        lastResult = null
        revealUntilAsk()
    }

    /**
     * Показывает реплики персонажей ПО ОДНОЙ (сначала индикатор «печатает…»,
     * затем сама реплика с озвучкой), пока не дойдём до хода курьера.
     */
    private fun revealUntilAsk() {
        revealJob?.cancel()
        revealJob = viewModelScope.launch {
            while (true) {
                val s = session ?: return@launch
                when (val turn = s.turns.getOrNull(s.turnIndex)) {
                    null -> { session = s.copy(atEnd = true, typing = null); return@launch }
                    is Turn.Ask -> { session = s.copy(awaiting = true, typing = null); return@launch }
                    is Turn.Say -> {
                        // 1) показываем «… печатает»
                        session = s.copy(typing = npcLabel(turn.speaker))
                        delay(1000)
                        // 2) показываем саму реплику и озвучиваем
                        val cur = session ?: return@launch
                        val msg = ChatMsg(false, turn.et, turn.ru, npcLabel(turn.speaker))
                        session = cur.copy(
                            transcript = cur.transcript + msg,
                            turnIndex = cur.turnIndex + 1,
                            typing = null
                        )
                        tts.speak(turn.et, flush = false)
                        delay(350)
                    }
                }
            }
        }
    }

    fun cancelOrder() {
        revealJob?.cancel()
        session = null
    }

    /** Выбор варианта (до отправки). */
    fun select(index: Int) {
        val s = session ?: return
        if (!s.awaiting || index in s.wrongSelected) return
        session = s.copy(selected = index)
    }

    /** Отправить выбранный вариант как сообщение курьера. */
    fun confirm() {
        val s = session ?: return
        if (!s.awaiting) return
        val ask = s.currentAsk ?: return
        val i = s.selected ?: return
        val choice = ask.choices[i]

        if (!choice.correct) {
            state = state.copy(wrong = state.wrong + 1)
            repo.save(state)
            session = s.copy(
                wrongSelected = s.wrongSelected + i,
                selected = null,
                mistakes = s.mistakes + 1
            )
            return
        }

        val msgs = s.transcript + ChatMsg(true, choice.et, choice.ru, "🛵 Вы")
        state = state.copy(
            learnedIds = state.learnedIds + ask.teachWordIds,
            correct = state.correct + 1
        )
        repo.save(state)
        session = s.copy(
            transcript = msgs,
            turnIndex = s.turnIndex + 1,
            awaiting = false,
            selected = null,
            wrongSelected = emptySet(),
            correctCount = s.correctCount + 1
        )
        tts.speak(choice.et, flush = true)   // озвучить реплику курьера
        revealUntilAsk()                     // затем показать ответ персонажа
    }

    /** Кнопка «Завершить доставку» в самом конце диалога. */
    fun finishDelivery() {
        val s = session ?: return
        if (s.atEnd) finishOrder(s)
    }

    private fun finishOrder(s: OrderSession) {
        val perfect = s.mistakes == 0
        val tip = if (perfect) s.order.payout * 0.25 else 0.0
        val earned = s.order.payout + tip
        val xpGain = s.correctCount * 20 + if (perfect) 30 else 0

        val ratingDelta = if (perfect) 0.02 else -0.05 * s.mistakes
        val newRating = min(5.0, max(3.5, state.rating + ratingDelta))

        state = state.copy(
            money = state.money + earned,
            xp = state.xp + xpGain,
            deliveries = state.deliveries + 1,
            rating = newRating
        )
        repo.save(state)

        lastResult = DeliveryResult(
            order = s.order,
            steps = s.correctCount,
            mistakes = s.mistakes,
            earned = earned,
            tip = tip,
            xp = xpGain,
            perfect = perfect
        )
        session = s.copy(finished = true)
    }

    fun closeResult() {
        session = null
        lastResult = null
    }

    fun resetProgress() {
        revealJob?.cancel()
        repo.clear()
        state = GameState()
        session = null
        lastResult = null
        online = false
    }

    /** Сколько слов выучено в данной теме. */
    fun learnedInCategory(categoryId: String): Int {
        val ids = Content.categories.firstOrNull { it.id == categoryId }
            ?.words?.map { it.id }?.toSet() ?: emptySet()
        return state.learnedIds.count { it in ids }
    }

    fun moneyStr(): String = "%.2f €".format(state.money)
    fun ratingStr(): String = "%.2f".format(state.rating)

    override fun onCleared() {
        super.onCleared()
        revealJob?.cancel()
        tts.shutdown()
    }
}

data class DeliveryResult(
    val order: Order,
    val steps: Int,
    val mistakes: Int,
    val earned: Double,
    val tip: Double,
    val xp: Int,
    val perfect: Boolean
) {
    val earnedStr get() = "%.2f €".format(earned)
    val tipStr get() = "%.2f €".format(tip)
}
