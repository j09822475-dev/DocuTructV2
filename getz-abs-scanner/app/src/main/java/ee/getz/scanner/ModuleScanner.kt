package ee.getz.scanner

/**
 * Автоскан модулей (раздел 8 брифа): эмпирический read-only перебор адресов ЭБУ
 * по K-line протоколам. Использует ТОЛЬКО команды чтения:
 * StartCommunication (81), идентификация (1A xx), чтение DTC (18/13).
 * Никакой записи, актуаторных тестов, сбросов и программирования.
 */
class ModuleScanner(
    private val elm: Elm327,
    private val log: (String) -> Unit,
    private val isCancelled: () -> Boolean
) {

    data class FoundModule(
        val address: Int,
        val protocol: String,
        val headerFormat: String,
        val startResponse: String,
        val ident: Map<String, String>,
        val dtc: Map<String, String>
    )

    /** Форматные байты заголовка (подбор до первого успеха). */
    private val headerFormats = listOf("80", "81", "C0", "C1", "82")

    /** K-line протоколы: KWP fast, KWP 5-baud, ISO 9141-2. */
    private val protocols = listOf(
        "5" to "KWP2000 fast",
        "4" to "KWP2000 5-baud",
        "3" to "ISO 9141-2"
    )

    private val identCmds = listOf("1A 9B", "1A 87", "1A 80")
    private val dtcCmds = listOf("18 00 FF 00", "18 02 FF 00", "13")

    /** Курируемый список частых адресов. Функции — ПРЕДПОЛОЖИТЕЛЬНЫЕ (у Hyundai маппинг может отличаться). */
    val candidates: List<Pair<Int, String>> = listOf(
        0x01 to "двигатель (предпол.)",
        0x10 to "двигатель (предпол.)",
        0x11 to "двигатель (предпол.)",
        0x14 to "SRS-кандидат",
        0x15 to "SRS-кандидат",
        0x28 to "ABS-кандидат",
        0x29 to "ABS-кандидат",
        0x2B to "ABS-кандидат",
        0x38 to "климат (предпол.)",
        0x40 to "панель приборов (предпол.)",
        0x50 to "АКПП (предпол.)",
        0x61 to "прочее",
        0x90 to "иммобилайзер (предпол.)"
    )

    fun scanCandidates(): List<FoundModule> =
        scan(candidates.map { it.first }, candidates.toMap())

    fun scanFull(): List<FoundModule> =
        scan((0x01..0xFF).toList(), emptyMap())

    private fun scan(addresses: List<Int>, names: Map<Int, String>): List<FoundModule> {
        val found = ArrayList<FoundModule>()
        log("=== АВТОСКАН МОДУЛЕЙ (read-only) ===")
        log("Зажигание должно быть в положении ON. Скан может занять минуты.")

        outer@ for ((pNum, pName) in protocols) {
            if (isCancelled()) break
            log("--- Протокол $pName (ATSP$pNum) ---")
            // Сырой обмен: без эха, без LF, с заголовками, без CAN-форматирования
            for (c in listOf("ATE0", "ATL0", "ATH1", "ATCAF0", "ATSP$pNum")) elm.send(c, 2000)

            val remaining = addresses.filter { a -> found.none { it.address == a } }
            for ((i, addr) in remaining.withIndex()) {
                if (isCancelled()) { log("Скан остановлен пользователем."); break@outer }
                val addrHex = String.format("%02X", addr)
                if (remaining.size > 20 && i % 16 == 0) {
                    log("…$pName: адрес $addrHex (${i + 1}/${remaining.size})")
                }
                for (fmt in headerFormats) {
                    if (isCancelled()) break@outer
                    elm.send("ATSH $fmt $addrHex F1", 2000)
                    // 5-baud инициализация медленная — таймаут с запасом
                    val start = elm.send("81", if (pNum == "4") 12000 else 8000)
                    if (!Elm327.isAlive(start)) continue

                    val name = names[addr]?.let { " ($it)" } ?: ""
                    log("✔ ЖИВОЙ БЛОК: адрес $addrHex$name, $pName, формат $fmt")
                    log("   Ответ на 81: $start")

                    val ident = LinkedHashMap<String, String>()
                    for (ic in identCmds) {
                        if (isCancelled()) break
                        val r = elm.send(ic, 6000)
                        ident[ic] = r
                        if (Elm327.isAlive(r)) log("   $ic → $r")
                    }
                    val dtc = LinkedHashMap<String, String>()
                    for (dc in dtcCmds) {
                        if (isCancelled()) break
                        val r = elm.send(dc, 6000)
                        dtc[dc] = r
                        if (Elm327.isAlive(r)) log("   $dc → $r")
                    }
                    found.add(FoundModule(addr, pName, fmt, start, ident, dtc))
                    break   // формат подобран — дальше не перебираем
                }
            }
        }

        // Вернуть адаптер в исходное состояние
        for (c in listOf("ATSP0", "ATE0", "ATL0", "ATS0", "ATH1")) elm.send(c, 2000)

        log("=== ИТОГ СКАНА ===")
        if (found.isEmpty()) {
            log("Живых блоков не найдено. Возможные причины:")
            log("• зажигание выключено (нужно положение ON);")
            log("• адаптер-клон не поднимает K-line с не-OBD модулем (см. «Проверка адаптера»);")
            log("• другой адрес/протокол — попробуйте полный свип 01–FF.")
        } else {
            log("Найдено блоков: ${found.size}")
            for (m in found) {
                log("• ${String.format("%02X", m.address)} — ${m.protocol}, формат ${m.headerFormat}")
            }
        }
        return found
    }
}
