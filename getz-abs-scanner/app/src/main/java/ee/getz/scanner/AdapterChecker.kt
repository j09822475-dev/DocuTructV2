package ee.getz.scanner

/**
 * Расширенная проверка адаптера (раздел 6 брифа).
 * Не верит строке версии: гоняет полный набор AT-команд, маркерные команды,
 * критичные для ABS возможности и поведенческие тесты, затем выносит вердикт.
 */
class AdapterChecker(
    private val elm: Elm327,
    private val log: (String) -> Unit,
    private val isCancelled: () -> Boolean
) {

    enum class Verdict { GODEN, USLOVNO, NEPRIGODEN }

    data class Report(
        val version: String,
        val device: String,
        val deviceId: String,
        val voltage: String,
        val supported: Int,
        val total: Int,
        val vehicleOnly: Int,
        val skipped: Int,
        val markerResults: Map<String, Boolean>,
        val absResults: Map<String, Boolean>,
        val suspiciousVersion: Boolean,
        val instantNoData: Boolean?,   // null = тест неубедителен (нет связи с авто)
        val truncatesLong: Boolean?,
        val verdict: Verdict,
        val text: String
    )

    fun run(): Report? {
        log("=== ПРОВЕРКА АДАПТЕРА ===")

        // 6.2. Идентификаторы (справочно, не для вердикта)
        val version = elm.send("ATI")
        val device = elm.send("AT@1")
        val deviceId = elm.send("AT@2")
        val voltage = elm.send("ATRV")
        log("Версия (ATI): $version")
        log("Устройство (AT@1): $device")
        log("Идентификатор (AT@2): $deviceId")
        log("Напряжение (ATRV): $voltage")
        if (!voltage.uppercase().contains("V")) {
            log("⚠ Напряжение не читается — адаптер не в разъёме OBD или зажигание выключено.")
        }

        // 6.3. Полный прогон AT-команд
        log("--- Прогон AT-команд (${AtCommands.SWEEP.size} шт.) ---")
        var supported = 0
        var executed = 0
        var vehicleOnly = 0
        var skipped = 0
        val details = StringBuilder()
        for (c in AtCommands.SWEEP) {
            if (isCancelled()) { log("Проверка остановлена."); return null }
            when {
                c.unsafe -> {
                    skipped++
                    details.append("⏭ ${c.cmd} — пропущена (небезопасна для автопрогона)\n")
                }
                c.needsVehicle -> {
                    vehicleOnly++
                    details.append("🚗 ${c.cmd} — требует связи с авто, не проверяется\n")
                }
                else -> {
                    val resp = elm.send(c.cmd, 3000)
                    executed++
                    val ok = Elm327.isRecognised(resp)
                    if (ok) supported++
                    details.append("${if (ok) "✔" else "✘"} ${c.cmd} (${c.desc}) → ${resp.take(40)}\n")
                }
            }
        }
        log("Поддержано $supported из $executed проверенных " +
            "(ещё $vehicleOnly требуют авто, $skipped пропущено).")

        // Восстановить рабочее состояние после ATD/ATSP-экспериментов свипа
        reinit()

        // 6.4. Маркерные команды
        log("--- Маркерные команды (детекторы клона) ---")
        val markerResults = LinkedHashMap<String, Boolean>()
        for (m in AtCommands.MARKERS) {
            if (isCancelled()) return null
            val resp = elm.send(m, 3000)
            val ok = if (m == "ATPPS")
                Regex("[0-9A-F]{2}").containsMatchIn(resp.uppercase()) && !resp.contains("?")
            else Elm327.isRecognised(resp)
            markerResults[m] = ok
            log("${if (ok) "✔" else "✘"} $m → ${resp.take(60)}")
        }
        if (markerResults.values.any { !it }) {
            log("⚠ Часть маркерных команд не поддержана — сильный признак клона.")
        }

        // 6.5. Критичные для ABS возможности
        log("--- Возможности, критичные для ABS ---")
        val absResults = LinkedHashMap<String, Boolean>()
        for ((cmd, title) in AtCommands.ABS_CRITICAL) {
            if (isCancelled()) return null
            val resp = elm.send(cmd, 3000)
            val ok = Elm327.isRecognised(resp) && !resp.uppercase().contains("ERROR")
            absResults[title] = ok
            log("${if (ok) "✔" else "✘"} $title ($cmd) → ${resp.take(40)}")
        }
        elm.send("ATSP0")   // не оставлять адаптер в ручном протоколе
        reinit()

        // 6.6. Поведенческие тесты
        log("--- Поведенческие тесты ---")
        var instantNoData: Boolean? = null
        val r0100 = elm.send("0100", 15000)
        val dt = elm.lastFirstByteMs
        val up0100 = r0100.uppercase()
        when {
            up0100.contains("NO DATA") -> {
                // Настоящий чип сначала ищет протокол (SEARCHING) и ждёт ответа ЭБУ;
                // клон отвечает NO DATA сразу, без поиска.
                instantNoData = !up0100.contains("SEARCHING") && dt in 0..49
                log("Тест «мгновенный NO DATA»: первый байт за ${dt} мс → " +
                    if (instantNoData == true) "⚠ подозрительно быстро (клон не ждёт ЭБУ)"
                    else "норма")
            }
            up0100.contains("UNABLE") || up0100.contains("ERROR") ->
                log("Тест «мгновенный NO DATA»: нет связи с авто — тест неубедителен.")
            else ->
                log("Тест «мгновенный NO DATA»: получен ответ от авто (${r0100.take(30)}) — ок.")
        }

        var truncatesLong: Boolean? = null
        val longCmd = "18 00 FF 00"   // read-only, 4 байта — клоны «под OBD» обрезают >2 байт
        val rLong = elm.send(longCmd, 8000)
        val upLong = rLong.uppercase()
        when {
            upLong.contains("?") -> {
                truncatesLong = true
                log("Тест длинной команды ($longCmd): ответ '?' → ⚠ адаптер обрезает длинные команды.")
            }
            upLong.isEmpty() && elm.lastTimedOut -> {
                log("Тест длинной команды: таймаут — неубедительно.")
            }
            else -> {
                truncatesLong = false
                log("Тест длинной команды ($longCmd): команда принята (${rLong.take(30)}) — ок.")
            }
        }
        reinit()

        // 6.7. Эвристика версии
        val suspiciousVersion = version.contains("1.5") || version.contains("2.1")
        if (suspiciousVersion) {
            log("⚠ Строка версии «$version» типична для клонов (v1.5/v2.1) — флаг, не приговор.")
        }

        // 6.8. Вердикт
        val hasAtsh = absResults["ручные заголовки (ATSH)"] == true
        val hasKline = (absResults["KWP2000 fast (ISO 14230-4)"] == true) ||
                (absResults["KWP2000 5-baud"] == true) ||
                (absResults["ISO 9141-2"] == true)
        val cloneSigns = markerResults.values.any { !it } || suspiciousVersion ||
                instantNoData == true || truncatesLong == true

        val verdict = when {
            !hasAtsh || !hasKline -> Verdict.NEPRIGODEN
            cloneSigns -> Verdict.USLOVNO
            else -> Verdict.GODEN
        }

        val verdictText = when (verdict) {
            Verdict.NEPRIGODEN ->
                "✘ НЕПРИГОДЕН для ABS: адаптер не принимает ручные заголовки (ATSH) " +
                "или не поддерживает ни один K-line протокол."
            Verdict.USLOVNO ->
                "△ УСЛОВНО ГОДЕН: базовые команды работают, но есть признаки клона. " +
                "Для двигателя ок, ABS — как повезёт."
            Verdict.GODEN ->
                "✔ ГОДЕН: заголовки и K-line поддерживаются, маркерные команды в норме."
        }

        val report = buildString {
            appendLine("===== ОТЧЁТ О ПРОВЕРКЕ АДАПТЕРА =====")
            appendLine("Версия: $version")
            appendLine("Устройство: $device / $deviceId")
            appendLine("Напряжение: $voltage")
            appendLine("AT-команды: поддержано $supported из $executed " +
                "(требуют авто: $vehicleOnly, пропущено: $skipped)")
            for ((m, ok) in markerResults) appendLine("Маркер $m: ${if (ok) "OK" else "НЕТ"}")
            for ((t, ok) in absResults) appendLine("$t: ${if (ok) "OK" else "НЕТ"}")
            appendLine("Подозрительная версия: ${if (suspiciousVersion) "да" else "нет"}")
            appendLine("Мгновенный NO DATA: ${triState(instantNoData)}")
            appendLine("Обрезает длинные команды: ${triState(truncatesLong)}")
            appendLine("ВЕРДИКТ: $verdictText")
            appendLine()
            appendLine("--- Детали прогона AT-команд ---")
            append(details)
        }
        log(verdictText)
        log("Полный отчёт сформирован — используйте «Экспорт лога» для сохранения.")
        log(report)

        return Report(
            version, device, deviceId, voltage,
            supported, executed, vehicleOnly, skipped,
            markerResults, absResults, suspiciousVersion,
            instantNoData, truncatesLong, verdict, report
        )
    }

    private fun triState(v: Boolean?) = when (v) {
        true -> "да"
        false -> "нет"
        null -> "тест неубедителен"
    }

    /** Вернуть адаптер в рабочее состояние приложения. */
    private fun reinit() {
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH1", "ATSP0")) elm.send(c, 2000)
    }
}
