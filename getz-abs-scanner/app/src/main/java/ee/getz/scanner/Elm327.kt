package ee.getz.scanner

import android.bluetooth.BluetoothSocket
import java.io.IOException

/**
 * Обёртка над Bluetooth-сокетом ELM327.
 *
 * Все команды выполняются строго последовательно (одна шина — один запрос):
 * вызывающий код обязан ходить сюда из одного рабочего потока.
 * Ответ читается до символа приглашения '>' либо до таймаута.
 */
class Elm327(private val socket: BluetoothSocket) {

    private val input = socket.inputStream
    private val output = socket.outputStream

    /** Задержка до первого байта ответа последней команды, мс (-1 — ответа не было). */
    @Volatile
    var lastFirstByteMs: Long = -1
        private set

    /** true, если последняя команда завершилась по таймауту (не дождались '>'). */
    @Volatile
    var lastTimedOut: Boolean = false
        private set

    /**
     * Отправить команду и прочитать ответ до '>' (символ приглашения не включается).
     * Перед отправкой входной буфер очищается от «хвостов» предыдущих ответов.
     */
    @Throws(IOException::class)
    fun send(cmd: String, timeoutMs: Long = 5000): String {
        drainInput()
        output.write((cmd + "\r").toByteArray(Charsets.US_ASCII))
        output.flush()

        val sb = StringBuilder()
        val start = System.currentTimeMillis()
        lastFirstByteMs = -1
        lastTimedOut = false

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (input.available() > 0) {
                val b = input.read()
                if (b < 0) break
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

    /** Очистить входной буфер (например, после прерванного monitor-режима). */
    fun drainInput() {
        try {
            while (input.available() > 0) input.read()
        } catch (_: IOException) { }
    }

    /** Убрать эхо/пустые строки/CR, склеить в удобочитаемый вид. */
    private fun clean(sb: StringBuilder): String =
        sb.toString()
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    fun close() {
        try { socket.close() } catch (_: IOException) { }
    }

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
