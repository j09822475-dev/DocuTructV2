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
import ee.kuller.app.data.OrderFactory
import ee.kuller.app.model.Nav
import ee.kuller.app.model.NavOption
import ee.kuller.app.model.Order
import ee.kuller.app.model.Phase
import ee.kuller.app.model.Thread
import ee.kuller.app.model.Turn
import ee.kuller.app.util.Speaker as Tts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

fun threadLabel(thread: Thread): String = when (thread) {
    Thread.RESTORAN -> "🏪 Ресторан"
    Thread.KLIENT -> "🙋 Клиент"
    Thread.TUGI -> "🎧 Поддержка"
}

data class ChatMsg(
    val thread: Thread,
    val fromCourier: Boolean,
    val et: String,
    val ru: String,
    val label: String
)

val THREAD_ORDER = listOf(Thread.RESTORAN, Thread.KLIENT, Thread.TUGI)

data class OrderSession(
    val order: Order,
    val phases: Map<String, Phase>,
    val phaseId: String,
    val turns: List<Turn>,
    val turnIndex: Int = 0,
    val transcript: List<ChatMsg> = emptyList(),
    val activeThread: Thread = Thread.RESTORAN,
    val threadsSeen: Set<Thread> = setOf(Thread.RESTORAN),
    val unread: Set<Thread> = emptySet(),
    val awaiting: Boolean = false,
    val nav: Nav.Choose? = null,
    val atEnd: Boolean = false,
    val typing: Thread? = null,
    val selected: Int? = null,
    val correctCount: Int = 0,
    val mistakes: Int = 0,
    val ratingBonus: Double = 0.0,
    val finished: Boolean = false
) {
    val currentAsk: Turn.Ask? get() = turns.getOrNull(turnIndex) as? Turn.Ask
    val progress: Float get() = if (turns.isEmpty()) 0f else turnIndex.toFloat() / turns.size
    fun messages(thread: Thread): List<ChatMsg> = transcript.filter { it.thread == thread }
    val tabs: List<Thread> get() = THREAD_ORDER.filter { it in threadsSeen }
}

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository(app)
    private val tts = Tts(app)
    private var revealJob: Job? = null

    var state by mutableStateOf(repo.load()); private set
    var online by mutableStateOf(false); private set
    var session by mutableStateOf<OrderSession?>(null); private set
    var availableOrders by mutableStateOf<List<Order>>(emptyList()); private set
    var lastResult by mutableStateOf<DeliveryResult?>(null); private set

    fun toggleOnline() { online = !online; if (online) refreshOrders() }
    fun refreshOrders() { availableOrders = OrderFactory.batch(5) }
    fun speak(text: String) = tts.speak(text, flush = true)

    fun startOrder(order: Order) {
        revealJob?.cancel()
        val delivery = DialogueFactory.build(order)
        // Перемешиваем варианты во всех фазах.
        val phases = delivery.phases.mapValues { (_, ph) ->
            ph.copy(turns = ph.turns.map {
                if (it is Turn.Ask) it.copy(choices = it.choices.shuffled()) else it
            })
        }
        val start = phases.getValue(delivery.start)
        session = OrderSession(
            order = order, phases = phases, phaseId = delivery.start,
            turns = start.turns, activeThread = start.thread,
            threadsSeen = setOf(start.thread)
        )
        lastResult = null
        revealUntilAsk()
    }

    private fun revealUntilAsk() {
        revealJob?.cancel()
        revealJob = viewModelScope.launch {
            while (true) {
                val s = session ?: return@launch
                when (val turn = s.turns.getOrNull(s.turnIndex)) {
                    null -> {
                        // Фаза закончилась → навигация.
                        when (val nav = s.phases[s.phaseId]?.nav) {
                            is Nav.Choose -> session = s.copy(nav = nav, typing = null, awaiting = false)
                            else -> session = s.copy(atEnd = true, typing = null, awaiting = false, nav = null)
                        }
                        return@launch
                    }
                    is Turn.Ask -> { session = s.copy(awaiting = true, typing = null); return@launch }
                    is Turn.Say -> {
                        session = s.copy(typing = turn.thread)
                        delay(1000)
                        val cur = session ?: return@launch
                        val msg = ChatMsg(turn.thread, false, turn.et, turn.ru, threadLabel(turn.thread))
                        session = cur.copy(
                            transcript = cur.transcript + msg,
                            turnIndex = cur.turnIndex + 1,
                            typing = null,
                            unread = if (turn.thread == cur.activeThread) cur.unread else cur.unread + turn.thread
                        )
                        tts.speak(turn.et, flush = false)
                        delay(300)
                    }
                }
            }
        }
    }

    fun setActiveThread(thread: Thread) {
        val s = session ?: return
        session = s.copy(activeThread = thread, unread = s.unread - thread)
    }

    fun select(index: Int) {
        val s = session ?: return
        if (!s.awaiting) return
        session = s.copy(selected = index)
    }

    fun confirm() {
        val s = session ?: return
        if (!s.awaiting) return
        val ask = s.currentAsk ?: return
        val i = s.selected ?: return
        val choice = ask.choices[i]

        val msg = ChatMsg(ask.thread, true, choice.et, choice.ru, "🛵 Вы")
        state = state.copy(
            learnedIds = if (choice.correct) state.learnedIds + ask.teachWordIds else state.learnedIds,
            correct = state.correct + if (choice.correct) 1 else 0,
            wrong = state.wrong + if (choice.correct) 0 else 1
        )
        repo.save(state)

        // Любой вариант рабочий: вставляем его ветку и идём дальше.
        val newTurns = buildList {
            addAll(s.turns.subList(0, s.turnIndex + 1))
            addAll(choice.followUp)
            addAll(s.turns.subList(s.turnIndex + 1, s.turns.size))
        }
        session = s.copy(
            turns = newTurns,
            transcript = s.transcript + msg,
            turnIndex = s.turnIndex + 1,
            awaiting = false,
            selected = null,
            correctCount = s.correctCount + if (choice.correct) 1 else 0,
            mistakes = s.mistakes + if (choice.correct) 0 else 1,
            ratingBonus = s.ratingBonus + choice.ratingDelta
        )
        tts.speak(choice.et, flush = true)
        revealUntilAsk()
    }

    /** Курьер выбрал, что делать дальше (какой чат открыть). */
    fun navChoose(option: NavOption) {
        val s = session ?: return
        val phase = s.phases[option.phase] ?: return
        session = s.copy(
            phaseId = option.phase,
            turns = phase.turns,
            turnIndex = 0,
            activeThread = phase.thread,
            threadsSeen = s.threadsSeen + phase.thread,
            unread = s.unread - phase.thread,
            nav = null,
            awaiting = false,
            selected = null
        )
        revealUntilAsk()
    }

    fun finishDelivery() {
        val s = session ?: return
        if (s.atEnd) finishOrder(s)
    }

    private fun finishOrder(s: OrderSession) {
        val perfect = s.mistakes == 0
        val tip = if (perfect) s.order.payout * 0.25 else 0.0
        val earned = s.order.payout + tip
        val xpGain = s.correctCount * 20 + if (perfect) 30 else 0
        val baseDelta = if (perfect) 0.02 else -0.02 * s.mistakes
        val newRating = min(5.0, max(3.0, state.rating + baseDelta + s.ratingBonus))

        state = state.copy(
            money = state.money + earned,
            xp = state.xp + xpGain,
            deliveries = state.deliveries + 1,
            rating = newRating
        )
        repo.save(state)
        lastResult = DeliveryResult(
            order = s.order, steps = s.correctCount, mistakes = s.mistakes,
            earned = earned, tip = tip, xp = xpGain, perfect = perfect
        )
        session = s.copy(finished = true)
    }

    fun cancelOrder() { revealJob?.cancel(); session = null }

    fun closeResult() {
        session = null
        lastResult = null
        if (online) refreshOrders()
    }

    fun resetProgress() {
        revealJob?.cancel()
        repo.clear()
        state = GameState()
        session = null
        lastResult = null
        online = false
    }

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
