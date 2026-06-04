package ee.kuller.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.kuller.app.GameViewModel
import ee.kuller.app.ui.SpeakButton
import ee.kuller.app.util.Speaker
import ee.kuller.app.util.rememberSpeaker

@Composable
fun OrderScreen(vm: GameViewModel, onExit: () -> Unit) {
    val session = vm.session
    val speaker = rememberSpeaker()

    if (session == null) {
        onExit(); return
    }

    val result = vm.lastResult
    if (session.finished && result != null) {
        ResultView(vm, onExit)
        return
    }

    val step = session.step
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Шапка с прогрессом доставки
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.cancelOrder(); onExit() }) {
                Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                Text(" Отмена")
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Шаг ${session.stepIndex + 1}/${session.order.steps.size}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        LinearProgressIndicator(
            progress = { session.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "📍 ${session.order.restaurant} → ${session.order.customer}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            session.order.address,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(16.dp))
        NpcBubble(
            speakerLabel = when (step.speaker.name) {
                "RESTORAN" -> "🏪 Ресторан"
                "KLIENT" -> "🙋 Клиент"
                else -> "🧭 Навигатор"
            },
            et = step.npcEt,
            ru = step.npcRu,
            speaker = speaker
        )

        Spacer(Modifier.height(16.dp))
        Text(
            step.questionRu,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        step.choices.forEachIndexed { i, choice ->
            ChoiceRow(
                et = choice.et,
                ru = choice.ru,
                index = i,
                selected = session.selected == i,
                locked = session.locked,
                correct = choice.correct,
                onClick = { vm.select(i) },
                onSpeak = { speaker.speak(choice.et) }
            )
        }

        Spacer(Modifier.height(16.dp))
        if (!session.locked) {
            Button(
                onClick = { vm.confirm() },
                enabled = session.selected != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Проверить", fontWeight = FontWeight.Bold) }
        } else {
            val wasCorrect = step.choices[session.selected ?: 0].correct
            FeedbackBanner(wasCorrect)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.next() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (session.isLastStep) "Завершить доставку 🏁" else "Дальше →",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NpcBubble(speakerLabel: String, et: String, ru: String, speaker: Speaker) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(speakerLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    et,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SpeakButton(onClick = { speaker.speak(et) })
            }
            Text(ru, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ChoiceRow(
    et: String,
    ru: String,
    index: Int,
    selected: Boolean,
    locked: Boolean,
    correct: Boolean,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val border: Color = when {
        locked && correct -> scheme.primary
        locked && selected && !correct -> scheme.error
        selected -> scheme.primary
        else -> scheme.onSurface.copy(alpha = 0.12f)
    }
    val bg: Color = when {
        locked && correct -> scheme.primary.copy(alpha = 0.10f)
        locked && selected && !correct -> scheme.error.copy(alpha = 0.10f)
        selected -> scheme.primary.copy(alpha = 0.06f)
        else -> scheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = !locked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(et, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = scheme.onSurface)
                Text(ru, fontSize = 13.sp, color = scheme.onSurface.copy(alpha = 0.7f))
            }
            if (locked && correct) {
                Icon(Icons.Filled.Check, null, tint = scheme.primary)
            } else if (locked && selected && !correct) {
                Icon(Icons.Filled.Close, null, tint = scheme.error)
            } else {
                SpeakButton(onClick = onSpeak)
            }
        }
    }
}

@Composable
private fun FeedbackBanner(correct: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background((if (correct) scheme.primary else scheme.error).copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        Text(
            if (correct) "✅ Õige! Верно! +20 XP"
            else "❌ Vale. Неверно — правильный вариант подсвечен зелёным.",
            color = if (correct) scheme.primary else scheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultView(vm: GameViewModel, onExit: () -> Unit) {
    val r = vm.lastResult ?: return
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(if (r.perfect) "🏆" else "🏁", fontSize = 64.sp)
        Text(
            if (r.perfect) "Доставка без ошибок!" else "Доставка выполнена",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "${r.order.restaurant} → ${r.order.customer}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(20.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultRow("Правильных ответов", "${r.correct}/${r.total} (${r.percent}%)")
                ResultRow("Оплата доставки", "%.2f €".format(r.order.payout))
                ResultRow("Чаевые за качество", if (r.tip > 0) "+${r.tipStr}" else "—")
                ResultRow("Опыт", "+${r.xp} XP")
                Box(Modifier.fillMaxWidth().height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                ResultRow("Итого заработано", r.earnedStr, highlight = true)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { vm.closeResult(); onExit() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Дальше работать 🛵", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ResultRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (highlight) 16.sp else 14.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface)
        Text(value, fontSize = if (highlight) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
