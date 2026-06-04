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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Активная сессия доставки (один заказ в процессе). */
data class OrderSession(
    val order: Order,
    val stepIndex: Int = 0,
    val selected: Int? = null,    // выбранный вариант
    val locked: Boolean = false,  // ответ зафиксирован
    val correctCount: Int = 0,
    val mistakes: Int = 0,
    val finished: Boolean = false
) {
    val step get() = order.steps[stepIndex]
    val isLastStep get() = stepIndex == order.steps.lastIndex
    val progress get() = (stepIndex + if (locked) 1 else 0).toFloat() / order.steps.size
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
        session = OrderSession(order)
        lastResult = null
    }

    fun cancelOrder() { session = null }

    /** Выбор варианта (до фиксации). */
    fun select(index: Int) {
        val s = session ?: return
        if (s.locked) return
        session = s.copy(selected = index)
    }

    /** Зафиксировать ответ и проверить. */
    fun confirm() {
        val s = session ?: return
        val choiceIndex = s.selected ?: return
        if (s.locked) return
        val correct = s.step.choices[choiceIndex].correct

        // фиксируем выученные слова шага
        val learned = state.learnedIds + s.step.teachWordIds
        state = state.copy(
            learnedIds = learned,
            correct = state.correct + if (correct) 1 else 0,
            wrong = state.wrong + if (correct) 0 else 1
        )
        session = s.copy(
            locked = true,
            correctCount = s.correctCount + if (correct) 1 else 0,
            mistakes = s.mistakes + if (correct) 0 else 1
        )
        repo.save(state)
    }

    /** Перейти к следующему шагу или завершить заказ. */
    fun next() {
        val s = session ?: return
        if (!s.locked) return
        if (s.isLastStep) finishOrder(s) else session = s.copy(
            stepIndex = s.stepIndex + 1, selected = null, locked = false
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
            correct = s.correctCount,
            total = total,
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
    val correct: Int,
    val total: Int,
    val earned: Double,
    val tip: Double,
    val xp: Int,
    val perfect: Boolean
) {
    val earnedStr get() = "%.2f €".format(earned)
    val tipStr get() = "%.2f €".format(tip)
    val percent get() = (correct.toFloat() / total * 100).roundToInt()
}
