package ee.kuller.app.data

import android.content.Context

/** Постоянный прогресс игрока-курьера. */
data class GameState(
    val money: Double = 0.0,        // заработано €
    val xp: Int = 0,                // опыт (очки изучения)
    val deliveries: Int = 0,        // выполнено доставок
    val rating: Double = 5.0,       // рейтинг курьера
    val correct: Int = 0,           // верных ответов
    val wrong: Int = 0,             // ошибок
    val learnedIds: Set<String> = emptySet(), // выученные слова
    val reps: Map<String, Int> = emptyMap()   // сколько раз слово отработано (для скрытия перевода)
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
        learnedIds = prefs.getStringSet("learned", emptySet())?.toSet() ?: emptySet(),
        reps = (prefs.getString("reps", "") ?: "").split(";")
            .mapNotNull { e ->
                val p = e.split(":")
                if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
            }.toMap()
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
            .putString("reps", state.reps.entries.joinToString(";") { "${it.key}:${it.value}" })
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    fun loadHideRu(): Boolean = prefs.getBoolean("hideRu", false)
    fun saveHideRu(value: Boolean) = prefs.edit().putBoolean("hideRu", value).apply()
}
