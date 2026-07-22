package ee.getz.scanner

import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * Абстракция канала к ELM327: единый алгоритм «команда → чтение до '>'»
 * поверх разных транспортов (Bluetooth Classic SPP или BLE GATT).
 *
 * Все команды выполняются строго последовательно (одна шина — один запрос):
 * вызывающий код обязан ходить сюда из одного рабочего потока.
 */
abstract class Elm327 {

    /** Задержка до первого байта ответа последней команды, мс (-1 — ответа не было). */
    @Volatile
    var lastFirstByteMs: Long = -1
        protected set

    /** true, если последняя команда завершилась по таймауту (не дождались '>'). */
    @Volatile
    var lastTimedOut: Boolean = false
        protected set

    /** Отправить сырые байты в адаптер. */
    @Throws(IOException::class)
    protected abstract fun writeBytes(data: ByteArray)

    /** Следующий байт приёмного буфера или -1, если пока пусто (без блокировки). */
    @Throws(IOException::class)
    protected abstract fun tryReadByte(): Int

    /** Очистить входной буфер от «хвостов» предыдущих ответов. */
    abstract fun drainInput()

    abstract fun close()

    /**
     * Отправить команду и прочитать ответ до '>' (символ приглашения не включается).
     */
    @Throws(IOException::class)
    fun send(cmd: String, timeoutMs: Long = 5000): String {
        drainInput()
        writeBytes((cmd + "\r").toByteArray(Charsets.US_ASCII))

        val sb = StringBuilder()
        val start = System.currentTimeMillis()
        lastFirstByteMs = -1
        lastTimedOut = false

        while (System.currentTimeMillis() - start < timeoutMs) {
            val b = tryReadByte()
            if (b >= 0) {
                if (lastFirstByteMs < 0) lastFirstByteMs = System.currentTimeMillis() - start
                val c = b.toChar()
                if (c == '>') return clean(sb)
                sb.append(c)
            } else {
                try { Thread.sleep(5) } catch (_: InterruptedException) { break }
            }
        }
        lastTimedOut = true
        return clean(sb)
    }

    /** Убрать пустые строки и CR, склеить в удобочитаемый вид. */
    private fun clean(sb: StringBuilder): String =
        sb.toString()
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    companion object {
        /** Маркеры ошибочного/пустого ответа (раздел 8.4 брифа). */
        val ERROR_MARKERS = listOf(
            "NO DATA", "UNABLE TO CONNECT", "BUS INIT: ERROR", "BUS INIT:ERROR",
            "BUS ERROR", "CAN ERROR", "DATA ERROR", "ERROR", "STOPPED", "?", "SEARCHING"
        )

        /** Ответ считается «живым», если нет маркеров ошибки и есть хотя бы пара hex-цифр. */
        fun isAlive(resp: String): Boolean {
            val up = resp.uppercase()
            if (ERROR_MARKERS.any { up.contains(it) }) return false
            return Regex("[0-9A-F]{2}").containsMatchIn(up)
        }

        /** Ответ на AT-команду распознан адаптером (не '?'). */
        fun isRecognised(resp: String): Boolean {
            val t = resp.trim()
            return t.isNotEmpty() && t != "?" && !t.contains("?")
        }
    }
}

/** Транспорт Bluetooth Classic (SPP/RFCOMM). */
class Elm327Classic(private val socket: BluetoothSocket) : Elm327() {

    private val input = socket.inputStream
    private val output = socket.outputStream

    override fun writeBytes(data: ByteArray) {
        output.write(data)
        output.flush()
    }

    override fun tryReadByte(): Int =
        if (input.available() > 0) input.read() else -1

    override fun drainInput() {
        try {
            while (input.available() > 0) input.read()
        } catch (_: IOException) { }
    }

    override fun close() {
        try { socket.close() } catch (_: IOException) { }
    }
}
