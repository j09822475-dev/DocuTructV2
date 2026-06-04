package ee.kuller.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.kuller.app.data.Content
import ee.kuller.app.ui.SectionTitle
import ee.kuller.app.ui.WordRow
import ee.kuller.app.util.rememberSpeaker

/** Разговорник: готовые фразы курьера по темам, с озвучкой. */
@Composable
fun PhrasebookScreen() {
    val speaker = rememberSpeaker()
    // В разговорнике сначала самые нужные курьеру темы
    val order = listOf("phrase", "greet", "dir", "num", "food", "drink")
    val cats = order.mapNotNull { id -> Content.categories.firstOrNull { it.id == id } }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text(
                "Vestmik · Разговорник",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                "Нажми 🔊, чтобы услышать произношение",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        cats.forEach { cat ->
            item { SectionTitle("${cat.emoji}  ${cat.titleEt} · ${cat.titleRu}") }
            items(cat.words) { word ->
                WordRow(
                    et = word.et,
                    ru = word.ru,
                    example = word.example,
                    onSpeak = { speaker.speak(if (word.example.isNotBlank()) word.example else word.et) }
                )
            }
        }
        item { Text("", Modifier.padding(8.dp)) }
    }
}
