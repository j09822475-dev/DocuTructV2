package ee.kuller.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.kuller.app.ChatMsg
import ee.kuller.app.GameViewModel
import ee.kuller.app.threadLabel
import ee.kuller.app.ui.SpeakButton

@Composable
fun OrderScreen(vm: GameViewModel, onExit: () -> Unit) {
    val session = vm.session
    if (session == null) { onExit(); return }
    if (session.finished && vm.lastResult != null) { ResultView(vm, onExit); return }

    val messages = session.messages(session.activeThread)
    val showTyping = session.typing == session.activeThread
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, showTyping, session.awaiting, session.atEnd, session.activeThread, session.nav != null) {
        val extra = if (showTyping) 1 else 0
        if (messages.isNotEmpty() || showTyping) {
            listState.animateScrollToItem(messages.size + 1 + extra)
            listState.animateScrollToItem(messages.size + 1 + extra)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { vm.cancelOrder(); onExit() }) {
                Icon(Icons.Filled.Close, null, Modifier.size(18.dp)); Text(" Отмена")
            }
            Column(Modifier.weight(1f)) {
                Text("${session.order.restaurant} → ${session.order.customer}",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(session.order.address, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
        LinearProgressIndicator(progress = { session.progress },
            modifier = Modifier.fillMaxWidth().height(5.dp))

        // Вкладки-чаты
        if (session.tabs.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                session.tabs.forEach { t ->
                    FilterChip(
                        selected = t == session.activeThread,
                        onClick = { vm.setActiveThread(t) },
                        label = { Text(threadLabel(t) + if (t in session.unread) "  🔴" else "") }
                    )
                }
            }
        }

        // Лента активного чата
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            itemsIndexed(messages) { _, msg ->
                val ru = if (vm.hideRu) "" else vm.maskRu(msg.ru)
                ChatBubble(msg, ru) { vm.speak(msg.et, msg.fromCourier, msg.thread) }
            }
            if (showTyping) item { TypingBubble(threadLabel(session.activeThread)) }
            item { Spacer(Modifier.height(4.dp)) }
        }

        // Нижняя панель
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                val ask = session.currentAsk
                val nav = session.nav
                when {
                    session.atEnd -> {
                        Button(onClick = { vm.finishDelivery() },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Завершить доставку 🏁", fontWeight = FontWeight.Bold)
                        }
                    }
                    nav != null -> {
                        Text(nav.promptRu, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp))
                        nav.options.forEach { opt ->
                            Button(onClick = { vm.navChoose(opt) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp)) {
                                Text(opt.labelRu, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    session.awaiting && ask != null && ask.thread == session.activeThread -> {
                        // Прокручиваемые варианты + ЗАКРЕПЛЁННАЯ кнопка «Отправить»
                        Column(Modifier.heightIn(max = 270.dp).verticalScroll(rememberScrollState())) {
                            Text(ask.promptRu, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (ask.courier) "🛵 Выберите, что сказать:" else "🛵 Выберите ответ:",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                            )
                            ask.choices.forEachIndexed { i, choice ->
                                AnswerOption(
                                    et = choice.et, ru = choice.ru,
                                    selected = session.selected == i,
                                    onClick = { vm.select(i) },
                                    onSpeak = { vm.speak(choice.et, true, ask.thread) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { vm.confirm() },
                            enabled = session.selected != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            Text("  Отправить", fontWeight = FontWeight.Bold)
                        }
                    }
                    session.awaiting && ask != null -> {
                        Text("Ответьте в чате «${threadLabel(ask.thread)}»",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedButton(onClick = { vm.setActiveThread(ask.thread) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Открыть «${threadLabel(ask.thread)}»")
                        }
                    }
                    else -> {
                        Text("💬 …", fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg, displayRu: String, onSpeak: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val courier = msg.fromCourier
    val bubbleColor = if (courier) scheme.primary else scheme.surfaceVariant
    val onBubble = if (courier) scheme.onPrimary else scheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (courier) 18.dp else 4.dp,
        bottomEnd = if (courier) 4.dp else 18.dp
    )
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = if (courier) Arrangement.End else Arrangement.Start) {
        Column(
            horizontalAlignment = if (courier) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(msg.label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = scheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp))
            Surface(color = bubbleColor, shape = shape) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.widthIn(max = 230.dp)) {
                        Text(msg.et, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onBubble)
                        if (displayRu.isNotBlank()) {
                            Text(displayRu, fontSize = 13.sp, color = onBubble.copy(alpha = 0.8f))
                        }
                    }
                    SpeakButton(onClick = onSpeak, tint = if (courier) scheme.onPrimary else scheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble(label: String) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = scheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp))
        Surface(color = scheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)) {
            Text("печатает •••", fontSize = 14.sp,
                color = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun AnswerOption(
    et: String, ru: String, selected: Boolean,
    onClick: () -> Unit, onSpeak: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val border: Color = if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.12f)
    val bg: Color = if (selected) scheme.primary.copy(alpha = 0.08f) else scheme.surface
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(et, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = scheme.onSurface)
                Text(ru, fontSize = 12.sp, color = scheme.onSurface.copy(alpha = 0.65f))
            }
            SpeakButton(onClick = onSpeak)
        }
    }
}

@Composable
private fun ResultView(vm: GameViewModel, onExit: () -> Unit) {
    val r = vm.lastResult ?: return
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(if (r.perfect) "🏆" else "🏁", fontSize = 64.sp)
        Text(if (r.perfect) "Доставка без ошибок!" else "Доставка выполнена",
            fontWeight = FontWeight.Bold, fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Text("${r.order.restaurant} → ${r.order.customer}", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultRow("Верных ответов", "${r.steps}")
                ResultRow("Ошибок", if (r.mistakes == 0) "нет 🎯" else "${r.mistakes}")
                ResultRow("Оплата доставки", "%.2f €".format(r.order.payout))
                ResultRow("Чаевые за качество", if (r.tip > 0) "+${r.tipStr}" else "—")
                ResultRow("Опыт", "+${r.xp} XP")
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                ResultRow("Итого заработано", r.earnedStr, highlight = true)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.closeResult(); onExit() },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Дальше работать 🛵", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (highlight) 16.sp else 14.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface)
        Text(value, fontSize = if (highlight) 18.sp else 14.sp, fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
