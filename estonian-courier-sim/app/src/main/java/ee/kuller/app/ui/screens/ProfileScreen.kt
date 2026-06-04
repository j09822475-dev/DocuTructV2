package ee.kuller.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.kuller.app.GameViewModel
import ee.kuller.app.data.Content
import ee.kuller.app.ui.StatPill

@Composable
fun ProfileScreen(vm: GameViewModel) {
    val s = vm.state
    var confirmReset by remember { mutableStateOf(false) }
    val totalWords = Content.allWords.size

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Text(
            "Profiil · Профиль",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Уровень
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Курьер уровня ${s.level}", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${s.xpInLevel}/100 XP до следующего уровня", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                LinearProgressIndicator(
                    progress = { s.xpInLevel / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("💶", vm.moneyStr(), "Заработок", Modifier.weight(1f))
            StatPill("📦", "${s.deliveries}", "Доставки", Modifier.weight(1f))
            StatPill("⭐", vm.ratingStr(), "Рейтинг", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill("🎯", "${s.accuracy}%", "Точность", Modifier.weight(1f))
            StatPill("📚", "${s.learnedIds.size}/$totalWords", "Слова", Modifier.weight(1f))
            StatPill("✅", "${s.correct}", "Ответы", Modifier.weight(1f))
        }

        Text(
            "Прогресс по темам",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )
        Content.categories.forEach { cat ->
            val learned = vm.learnedInCategory(cat.id)
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${cat.emoji} ${cat.titleRu}", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("$learned/${cat.words.size}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    LinearProgressIndicator(
                        progress = { if (cat.words.isEmpty()) 0f else learned.toFloat() / cat.words.size },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { if (confirmReset) vm.resetProgress().also { confirmReset = false } else confirmReset = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
        ) {
            Text(if (confirmReset) "Точно сбросить весь прогресс?" else "Сбросить прогресс")
        }
    }
}
