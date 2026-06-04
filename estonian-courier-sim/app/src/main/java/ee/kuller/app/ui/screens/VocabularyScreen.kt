package ee.kuller.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import ee.kuller.app.ui.WordRow
import ee.kuller.app.util.rememberSpeaker

@Composable
fun VocabularyScreen(vm: GameViewModel) {
    val speaker = rememberSpeaker()
    var selected by remember { mutableStateOf(Content.categories.first().id) }
    val category = Content.categories.first { it.id == selected }
    val learned = vm.learnedInCategory(category.id)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Sõnavara · Словарь",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Content.categories.forEach { cat ->
                FilterChip(
                    selected = cat.id == selected,
                    onClick = { selected = cat.id },
                    label = { Text("${cat.emoji} ${cat.titleRu}") }
                )
            }
        }

        Text(
            "${category.titleEt} — выучено $learned/${category.words.size}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        LinearProgressIndicator(
            progress = { if (category.words.isEmpty()) 0f else learned.toFloat() / category.words.size },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(category.words) { word ->
                WordRow(
                    et = word.et,
                    ru = word.ru,
                    example = word.example,
                    onSpeak = { speaker.speak(word.et) }
                )
            }
            item { Text("", Modifier.padding(8.dp)) }
        }
    }
}
