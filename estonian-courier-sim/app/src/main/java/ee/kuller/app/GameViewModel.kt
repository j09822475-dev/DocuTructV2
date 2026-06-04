package ee.kuller.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ee.kuller.app.data.Content
import ee.kuller.app.data.GameRepository
import ee.kuller.app.data.GameState
import ee.kuller.app.model.Order
import ee.kuller.app.model.Speaker
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

/** Активная сессия доставки (один заказ в процессе) в виде чата. */
data class OrderSession(
    val order: Order,
    val stepIndex: Int = 0,
    val transcript: List<ChatMsg> = emptyList(),
    val selected: Int? = null,            // выбранный, но ещё не отправленный вариант
    val wrongSelected: Set<Int> = emptySet(), // ошибочно выбранные на этом шаге
    val answered: Boolean = false,        // на шаг дан верный ответ
    val correctCount: Int = 0,
    val mistakes: Int = 0,
    val finished: Boolean = false
) {
    val step get() = order.steps[stepIndex]
    val isLastStep get() = stepIndex == order.steps.lastIndex
    val progress get() = (stepIndex + if (answered) 1 else 0).toFloat() / order.steps.size
}

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository(app)

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

    fun startOrder(order: Order) {
        // Перемешиваем варианты ответов, чтобы верный не был всегда первым.
        val shuffled = order.copy(
            steps = order.steps.map { it.copy(choices = it.choices.shuffled()) }
        )
        val first = shuffled.steps.first()
        session = OrderSession(
            order = shuffled,
            transcript = openingMessages(first)
        )
        lastResult = null
    }

    private fun openingMessages(step: ee.kuller.app.model.DialogueStep): List<ChatMsg> =
        if (step.npcEt.isNotBlank())
            listOf(ChatMsg(false, step.npcEt, step.npcRu, npcLabel(step.speaker)))
        else emptyList()

    fun cancelOrder() { session = null }

    /** Выбор варианта (до отправки). */
    fun select(index: Int) {
        val s = session ?: return
        if (s.answered || index in s.wrongSelected) return
        session = s.copy(selected = index)
    }

    /** Отправить выбранный вариант как сообщение курьера. */
    fun confirm() {
        val s = session ?: return
        val i = s.selected ?: return
        if (s.answered) return
        val choice = s.step.choices[i]

        if (choice.correct) {
            val msgs = s.transcript.toMutableList()
            msgs += ChatMsg(true, choice.et, choice.ru, "🛵 Вы")
            if (s.step.npcReplyEt.isNotBlank()) {
                msgs += ChatMsg(false, s.step.npcReplyEt, s.step.npcReplyRu,
                    npcLabel(s.step.speaker) + " отвечает")
            }
            state = state.copy(
                learnedIds = state.learnedIds + s.step.teachWordIds,
                correct = state.correct + 1
            )
            repo.save(state)
            session = s.copy(
                transcript = msgs,
                answered = true,
                correctCount = s.correctCount + 1,
                selected = i
            )
        } else {
            state = state.copy(wrong = state.wrong + 1)
            repo.save(state)
            session = s.copy(
                wrongSelected = s.wrongSelected + i,
                selected = null,
                mistakes = s.mistakes + 1
            )
        }
    }

    /** Перейти к следующему шагу или завершить заказ. */
    fun next() {
        val s = session ?: return
        if (!s.answered) return
        if (s.isLastStep) { finishOrder(s); return }
        val nextIndex = s.stepIndex + 1
        val nextStep = s.order.steps[nextIndex]
        session = s.copy(
            stepIndex = nextIndex,
            transcript = s.transcript + openingMessages(nextStep),
            selected = null,
            wrongSelected = emptySet(),
            answered = false
        )
    }

    private fun finishOrder(s: OrderSession) {
        val total = s.order.steps.size
        val perfect = s.mistakes == 0
        // Чаевые зависят от точности (как реальный рейтинг курьера)
        val tip = if (perfect) s.order.payout * 0.25 else 0.0
        val earned = s.order.payout + tip
        val xpGain = s.correctCount * 20 + if (perfect) 30 else 0

        // рейтинг: за безошибочную доставку растёт, за ошибки слегка падает
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
            steps = total,
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
