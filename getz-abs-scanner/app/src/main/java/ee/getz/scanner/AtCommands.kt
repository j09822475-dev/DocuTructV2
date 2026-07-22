package ee.getz.scanner

/**
 * Справочный набор документированных AT-команд ELM327 (по даташиту, до v2.2)
 * для полного прогона в проверке адаптера (раздел 6.3 брифа).
 *
 * needsVehicle — команду нельзя честно проверить без установленной связи с авто
 * (она зависит от протокола/шины); в сводке помечается отдельно.
 * unsafe — команда исключена из автопрогона (сброс, сон, смена скорости UART,
 * monitor-режимы, запись в EEPROM): она может оборвать связь или изменить
 * состояние адаптера. Помечается «пропущена».
 */
data class AtCmd(
    val cmd: String,
    val desc: String,
    val needsVehicle: Boolean = false,
    val unsafe: Boolean = false
)

object AtCommands {

    val SWEEP: List<AtCmd> = listOf(
        // --- Общие / идентификация ---
        AtCmd("AT@1", "описание устройства"),
        AtCmd("AT@2", "идентификатор устройства"),
        AtCmd("ATI", "версия прошивки"),
        AtCmd("ATRV", "напряжение питания"),
        AtCmd("ATIGN", "уровень IgnMon (зажигание)"),
        AtCmd("ATDP", "текущий протокол (описание)"),
        AtCmd("ATDPN", "текущий протокол (номер)"),

        // --- Управление интерфейсом ---
        AtCmd("ATE0", "эхо выкл"),
        AtCmd("ATE1", "эхо вкл"),
        AtCmd("ATL0", "linefeed выкл"),
        AtCmd("ATL1", "linefeed вкл"),
        AtCmd("ATS0", "пробелы выкл"),
        AtCmd("ATS1", "пробелы вкл"),
        AtCmd("ATH0", "заголовки выкл"),
        AtCmd("ATH1", "заголовки вкл"),
        AtCmd("ATM0", "memory выкл"),
        AtCmd("ATM1", "memory вкл"),
        AtCmd("ATR0", "ответы выкл"),
        AtCmd("ATR1", "ответы вкл"),
        AtCmd("ATV0", "переменная длина DLC выкл"),
        AtCmd("ATV1", "переменная длина DLC вкл"),
        AtCmd("ATAT0", "adaptive timing выкл"),
        AtCmd("ATAT1", "adaptive timing 1"),
        AtCmd("ATAT2", "adaptive timing 2"),
        AtCmd("ATAL", "длинные сообщения (>7 байт)"),
        AtCmd("ATNL", "только стандартная длина"),

        // --- Программируемые параметры (маркерные — см. отдельный блок) ---
        AtCmd("ATPPS", "сводка programmable parameters"),
        AtCmd("ATPP 2A SV 38", "запись значения PP (без активации)"),
        AtCmd("ATPP FF OFF", "выключить все PP"),

        // --- Установка протокола ---
        AtCmd("ATSP0", "протокол авто"),
        AtCmd("ATSP1", "SAE J1850 PWM"),
        AtCmd("ATSP2", "SAE J1850 VPW"),
        AtCmd("ATSP3", "ISO 9141-2"),
        AtCmd("ATSP4", "KWP2000 5-baud"),
        AtCmd("ATSP5", "KWP2000 fast"),
        AtCmd("ATSP6", "CAN 11/500"),
        AtCmd("ATSP7", "CAN 29/500"),
        AtCmd("ATSP8", "CAN 11/250"),
        AtCmd("ATSP9", "CAN 29/250"),
        AtCmd("ATSP A6", "протокол 6 с авто-фолбэком"),
        AtCmd("ATTP6", "протокол 6 (пробно)"),
        AtCmd("ATTP A6", "протокол 6 пробно с фолбэком"),

        // --- Заголовки / адресация ---
        AtCmd("ATSH 80 10 F1", "заголовок 3 байта"),
        AtCmd("ATSH 6810F1", "заголовок 3 байта слитно"),
        AtCmd("ATSH 7E0", "заголовок CAN 11 бит"),
        AtCmd("ATCP 18", "приоритет CAN 29 бит"),
        AtCmd("ATSR F1", "адрес получателя (receiver)"),
        AtCmd("ATRA F1", "адрес приёма (receive address)"),
        AtCmd("ATTA F1", "tester address"),

        // --- Тайминги ---
        AtCmd("ATST 32", "таймаут ответа"),
        AtCmd("ATSW 92", "период wakeup-сообщений"),
        AtCmd("ATSW 00", "wakeup выкл"),
        AtCmd("ATBI", "пропустить инициализацию шины", needsVehicle = true),
        AtCmd("ATSI", "медленная (5-baud) инициализация", needsVehicle = true),
        AtCmd("ATFI", "быстрая инициализация", needsVehicle = true),
        AtCmd("ATKW", "показать key words", needsVehicle = true),
        AtCmd("ATKW0", "проверка key words выкл"),
        AtCmd("ATKW1", "проверка key words вкл"),

        // --- ISO / KWP ---
        AtCmd("ATIB 10", "ISO baud 10400"),
        AtCmd("ATIB 96", "ISO baud 9600"),
        AtCmd("ATIB 48", "ISO baud 4800"),
        AtCmd("ATIIA 33", "адрес ISO-инициализации"),
        AtCmd("ATWM 81 3F 3E", "тело wakeup-сообщения"),

        // --- CAN ---
        AtCmd("ATCAF0", "CAN авто-форматирование выкл"),
        AtCmd("ATCAF1", "CAN авто-форматирование вкл"),
        AtCmd("ATCFC0", "CAN flow control выкл"),
        AtCmd("ATCFC1", "CAN flow control вкл"),
        AtCmd("ATCF 7E8", "CAN фильтр 11 бит"),
        AtCmd("ATCM 7FF", "CAN маска 11 бит"),
        AtCmd("ATCRA 7E8", "CAN receive address"),
        AtCmd("ATCRA", "сброс CAN receive address"),
        AtCmd("ATCEA", "CAN extended address выкл"),
        AtCmd("ATCEA F1", "CAN extended address"),
        AtCmd("ATCS", "статус CAN"),
        AtCmd("ATCSM0", "CAN silent monitor выкл"),
        AtCmd("ATCSM1", "CAN silent monitor вкл"),
        AtCmd("ATCTM1", "таймер множитель 1"),
        AtCmd("ATCTM5", "таймер множитель 5"),
        AtCmd("ATCV 0000", "калибровка вольтметра сброс"),
        AtCmd("ATFC SM 0", "flow control режим 0"),
        AtCmd("ATFC SH 7E0", "flow control заголовок"),
        AtCmd("ATFC SD 30 00 00", "flow control данные"),
        AtCmd("ATPB C0 01", "параметры протокола B"),
        AtCmd("ATRTR", "отправить RTR", needsVehicle = true),
        AtCmd("ATDM1", "monitor DM1", needsVehicle = true, unsafe = true),

        // --- J1850 ---
        AtCmd("ATIFR0", "IFR выкл"),
        AtCmd("ATIFR1", "IFR авто"),
        AtCmd("ATIFR2", "IFR всегда"),
        AtCmd("ATIFR H", "IFR из заголовка"),
        AtCmd("ATIFR S", "IFR из источника"),

        // --- J1939 ---
        AtCmd("ATJE", "J1939 ELM-формат"),
        AtCmd("ATJS", "J1939 SAE-формат"),
        AtCmd("ATJHF0", "J1939 формат заголовка выкл"),
        AtCmd("ATJHF1", "J1939 формат заголовка вкл"),
        AtCmd("ATJTM1", "J1939 таймер x1"),
        AtCmd("ATJTM5", "J1939 таймер x5"),
        AtCmd("ATMP 0100", "monitor PGN", needsVehicle = true, unsafe = true),

        // --- Monitor-режимы (исключены из автопрогона) ---
        AtCmd("ATMA", "monitor all", unsafe = true),
        AtCmd("ATMR 10", "monitor receiver", unsafe = true),
        AtCmd("ATMT 68", "monitor transmitter", unsafe = true),

        // --- Прочее / состояние ---
        AtCmd("ATD", "восстановить умолчания"),
        AtCmd("ATD0", "показ DLC выкл"),
        AtCmd("ATD1", "показ DLC вкл"),
        AtCmd("ATBD", "buffer dump"),
        AtCmd("ATRD", "прочитать сохранённый байт"),
        AtCmd("ATSD 00", "сохранить байт (EEPROM)", unsafe = true),
        AtCmd("ATSS", "стандартный порядок поиска (SAE J1978)"),
        AtCmd("ATFE", "забыть события"),
        AtCmd("ATPC", "закрыть протокол"),
        AtCmd("ATBRD 45", "смена скорости UART", unsafe = true),
        AtCmd("ATBRT 0F", "таймаут смены скорости UART", unsafe = true),
        AtCmd("ATLP", "низкое энергопотребление (сон)", unsafe = true),
        AtCmd("ATWS", "тёплый рестарт", unsafe = true),
        AtCmd("ATZ", "полный сброс", unsafe = true)
    )

    /** Маркерные команды-детекторы клона (раздел 6.4). */
    val MARKERS = listOf("ATPPS", "ATPP 2A SV 38", "ATAL")

    /** Критичные для ABS команды (раздел 6.5): ручные заголовки + протоколы. */
    val ABS_CRITICAL = listOf(
        "ATSH 80 10 F1" to "ручные заголовки (ATSH)",
        "ATSP5" to "KWP2000 fast (ISO 14230-4)",
        "ATSP4" to "KWP2000 5-baud",
        "ATSP3" to "ISO 9141-2",
        "ATSP6" to "CAN 11/500 (ISO 15765)"
    )
}
