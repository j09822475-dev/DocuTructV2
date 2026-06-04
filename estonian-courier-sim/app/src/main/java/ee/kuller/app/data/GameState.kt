package ee.kuller.app.data

import android.content.Context

/** Постоянный прогресс игрока-курьера. */
data class GameState(
    val money: Double = 0.0,        // заработано €
    val xp: Int = 0,                // опыт (очки изучения)
    val deliveries: Int = 0,        // выполнено доставок
    val rating: Double = 5.0,       // рейтинг курьера (как в Bolt/Wolt)
    val correct: Int = 0,           // верных ответов
    val wrong: Int = 0,             // ошибок
    val learnedIds: Set<String> = emptySet() // выученные слова
) {
    val level: Int get() = xp / 100 + 1
    val xpInLevel: Int get() = xp % 100
    val accuracy: Int
        get() = if (correct + wrong == 0) 0 else (correct * 100) / (correct + wrong)
}

/** Простое хранилище прогресса в SharedPreferences. */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kuller_progress", Context.MODE_PRIVATE)

    fun load(): GameState = GameState(
        money = prefs.getFloat("money", 0f).toDouble(),
        xp = prefs.getInt("xp", 0),
        deliveries = prefs.getInt("deliveries", 0),
        rating = prefs.getFloat("rating", 5.0f).toDouble(),
        correct = prefs.getInt("correct", 0),
        wrong = prefs.getInt("wrong", 0),
        learnedIds = prefs.getStringSet("learned", emptySet())?.toSet() ?: emptySet()
    )

    fun save(state: GameState) {
        prefs.edit()
            .putFloat("money", state.money.toFloat())
            .putInt("xp", state.xp)
            .putInt("deliveries", state.deliveries)
            .putFloat("rating", state.rating.toFloat())
            .putInt("correct", state.correct)
            .putInt("wrong", state.wrong)
            .putStringSet("learned", state.learnedIds)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
