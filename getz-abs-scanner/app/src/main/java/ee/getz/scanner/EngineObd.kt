package ee.getz.scanner

/**
 * Диагностика двигателя по стандартному OBD-II (раздел 7 брифа).
 * Гарантированно рабочая часть: Mode 03 (чтение DTC), Mode 04 (сброс — только
 * по явному действию пользователя), живые PID (Mode 01).
 */
class EngineObd(
    private val elm: Elm327,
    private val log: (String) -> Unit
) {

    /** Живые параметры. Архитектура позволяет добавлять PID одной строкой. */
    data class Pid(
        val code: String,
        val name: String,
        val decode: (List<Int>) -> String
    )

    val livePids = listOf(
        Pid("010C", "Обороты") { b ->
            if (b.size >= 2) "${(b[0] * 256 + b[1]) / 4} об/мин" else "—"
        },
        Pid("0105", "Температура ОЖ") { b ->
            if (b.isNotEmpty()) "${b[0] - 40} °C" else "—"
        },
        Pid("010D", "Скорость") { b ->
            if (b.isNotEmpty()) "${b[0]} км/ч" else "—"
        },
        Pid("0104", "Нагрузка") { b ->
            if (b.isNotEmpty()) "${b[0] * 100 / 255} %" else "—"
        },
        Pid("010F", "Температура впуска") { b ->
            if (b.isNotEmpty()) "${b[0] - 40} °C" else "—"
        },
        Pid("0111", "Дроссель") { b ->
            if (b.isNotEmpty()) "${b[0] * 100 / 255} %" else "—"
        }
    )

    /** Инициализация под OBD-обмен: без заголовков, авто-протокол. */
    fun init() {
        log("=== ИНИЦИАЛИЗАЦИЯ OBD-II ===")
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) {
            val r = elm.send(c, 3000)
            log("$c → $r")
        }
    }

    fun readDtc() {
        log("=== ЧТЕНИЕ ОШИБОК ДВИГАТЕЛЯ (Mode 03) ===")
        init()
        val resp = elm.send("03", 15000)
        log("03 → $resp")
        val codes = parseDtc(resp)
        when {
            !Elm327.isAlive(resp) && codes.isEmpty() ->
                log("Нет связи с ЭБУ двигателя (зажигание? адаптер?).")
            codes.isEmpty() ->
                log("Сохранённых ошибок нет.")
            else -> {
                log("Найдено ошибок: ${codes.size}")
                codes.forEach { log("• $it") }
            }
        }
    }

    /** Сброс — только по явному действию пользователя (подтверждение — в UI). */
    fun clearDtc() {
        log("=== СБРОС ОШИБОК ДВИГАТЕЛЯ (Mode 04) ===")
        init()
        val resp = elm.send("04", 15000)
        log("04 → $resp")
        if (resp.uppercase().contains("44")) {
            log("Ошибки сброшены. Внимание: мониторы готовности тоже сброшены.")
        } else {
            log("Сброс не подтверждён ЭБУ.")
        }
    }

    fun readLive() {
        log("=== ЖИВЫЕ ПАРАМЕТРЫ ===")
        val v = elm.send("ATRV", 3000)
        log("Напряжение: $v")
        for (pid in livePids) {
            val resp = elm.send(pid.code, 8000)
            val bytes = parsePidBytes(resp, pid.code)
            if (bytes != null) {
                log("${pid.name}: ${pid.decode(bytes)}")
            } else {
                log("${pid.name}: нет данных ($resp)")
            }
        }
    }

    /**
     * Разобрать ответ Mode 01: найти "41 <pid>" и вернуть байты данных после него.
     * Ожидается работа с ATH0 (без заголовков), но лишние байты до 41 допустимы.
     */
    private fun parsePidBytes(resp: String, pidCode: String): List<Int>? {
        val hex = resp.uppercase().replace(Regex("[^0-9A-F]"), "")
        val pid = pidCode.substring(2)
        val marker = "41$pid"
        val idx = hex.indexOf(marker)
        if (idx < 0) return null
        val data = hex.substring(idx + marker.length)
        return data.chunked(2).filter { it.length == 2 }.map { it.toInt(16) }
    }

    /** Разобрать ответ Mode 03 в список кодов P/C/B/U. */
    fun parseDtc(resp: String): List<String> {
        val hex = resp.uppercase().replace(Regex("[^0-9A-F]"), "")
        val idx = hex.indexOf("43")
        if (idx < 0) return emptyList()
        val data = hex.substring(idx + 2)
        val out = ArrayList<String>()
        var i = 0
        while (i + 4 <= data.length) {
            val a = data.substring(i, i + 2).toInt(16)
            val b = data.substring(i + 2, i + 4)
            i += 4
            if (a == 0 && b == "00") continue
            val letter = when (a shr 6) {
                0 -> "P"; 1 -> "C"; 2 -> "B"; else -> "U"
            }
            val d1 = (a shr 4) and 0x3
            val d2 = a and 0xF
            out.add("$letter$d1${d2.toString(16).uppercase()}$b" +
                (DTC_DB[("$letter$d1${d2.toString(16).uppercase()}$b")]?.let { " — $it" } ?: ""))
        }
        return out
    }

    companion object {
        /** Небольшая база расшифровок частых кодов (расширяемая). */
        val DTC_DB = mapOf(
            "P0100" to "Расходомер воздуха — цепь",
            "P0105" to "Датчик абсолютного давления — цепь",
            "P0110" to "Датчик температуры впуска — цепь",
            "P0115" to "Датчик температуры ОЖ — цепь",
            "P0120" to "Датчик положения дросселя — цепь",
            "P0130" to "Лямбда-зонд 1 банк 1 — цепь",
            "P0133" to "Лямбда-зонд 1 банк 1 — медленный отклик",
            "P0135" to "Подогрев лямбда-зонда 1 — цепь",
            "P0171" to "Смесь слишком бедная (банк 1)",
            "P0172" to "Смесь слишком богатая (банк 1)",
            "P0201" to "Форсунка 1 — цепь",
            "P0300" to "Случайные пропуски зажигания",
            "P0301" to "Пропуски зажигания, цилиндр 1",
            "P0302" to "Пропуски зажигания, цилиндр 2",
            "P0303" to "Пропуски зажигания, цилиндр 3",
            "P0304" to "Пропуски зажигания, цилиндр 4",
            "P0325" to "Датчик детонации — цепь",
            "P0335" to "Датчик положения коленвала — цепь",
            "P0340" to "Датчик положения распредвала — цепь",
            "P0420" to "Эффективность катализатора ниже порога",
            "P0443" to "Клапан продувки адсорбера — цепь",
            "P0500" to "Датчик скорости — цепь",
            "P0505" to "Регулятор холостого хода",
            "P0560" to "Напряжение бортсети",
            "P0605" to "Ошибка ПЗУ ЭБУ",
            "P1102" to "Hyundai: лямбда-подогрев — сопротивление",
            "P1123" to "Hyundai: адаптация смеси — богатая",
            "P1124" to "Hyundai: адаптация смеси — бедная"
        )
    }
}
