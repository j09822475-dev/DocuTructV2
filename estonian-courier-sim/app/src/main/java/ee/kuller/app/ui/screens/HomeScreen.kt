package ee.kuller.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.kuller.app.GameViewModel
import ee.kuller.app.data.Content
import ee.kuller.app.model.Order
import ee.kuller.app.ui.StatPill

@Composable
fun HomeScreen(vm: GameViewModel, onAccept: (Order) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Tere, kuller! 🛵",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Учи эстонский, развозя заказы",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("💶", vm.moneyStr(), "Заработок", Modifier.weight(1f))
                StatPill("⭐", vm.ratingStr(), "Рейтинг", Modifier.weight(1f))
                StatPill("📦", "${vm.state.deliveries}", "Доставки", Modifier.weight(1f))
                StatPill("🎚", "Lvl ${vm.state.level}", "Уровень", Modifier.weight(1f))
            }
        }

        item { OnlineCard(vm) }

        if (vm.online) {
            item {
                Text(
                    "Доступные заказы",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(Content.orders) { order ->
                OrderCard(order, onAccept = { onAccept(order) })
            }
            item { Text("") }
        }
    }
}

@Composable
private fun OnlineCard(vm: GameViewModel) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vm.online) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.online) "Вы на линии" else "Вы офлайн",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (vm.online) "Принимайте заказы и учите слова"
                    else "Включите смену, чтобы получать заказы",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Switch(checked = vm.online, onCheckedChange = { vm.toggleOnline() })
        }
    }
}

@Composable
private fun OrderCard(order: Order, onAccept: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🍽", fontSize = 22.sp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(order.restaurant, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(order.itemsRu, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Text(
                    "+%.2f €".format(order.payout),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Place, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary)
                Text(order.address, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Text(" · %.1f km".format(order.distanceKm), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp))
                Text("  Принять заказ", fontWeight = FontWeight.Bold)
            }
        }
    }
}
