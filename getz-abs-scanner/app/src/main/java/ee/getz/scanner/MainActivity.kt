package ee.getz.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val REQ_BT = 42
    }

    // --- Связь ---
    private var btAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    @Volatile private var elm: Elm327? = null
    private var devices: List<BluetoothDevice> = emptyList()

    // Одна шина — один запрос: все команды через один последовательный поток
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val cancelled = AtomicBoolean(false)
    private val busy = AtomicBoolean(false)
    private val ui = Handler(Looper.getMainLooper())

    // --- UI ---
    private lateinit var deviceSpinner: Spinner
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var headerEdit: EditText
    private lateinit var cmdEdit: EditText
    private lateinit var protoSpinner: Spinner
    private lateinit var keepAliveBox: CheckBox
    private val logBuffer = StringBuilder()
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Getz ABS Scanner (ELM327)"
        btAdapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        setContentView(buildUi())
        log("Порядок работы: 1) Подключиться → 2) Проверить адаптер → 3) Работа с блоками.")
        log("Сопрягите ELM327 в настройках Bluetooth (PIN 1234 или 0000), затем «Обновить список».")
        startKeepAliveLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelled.set(true)
        disconnectInternal()
        executor.shutdownNow()
    }

    // ================= UI =================

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // --- 1. Подключение ---
        controls.addView(header("1. Подключение"))
        deviceSpinner = Spinner(this)
        controls.addView(deviceSpinner)
        controls.addView(buttonRow(
            "Обновить список" to { refreshDevices() },
            "Подключиться" to { connect() },
            "Отключиться" to { disconnect() }
        ))
        statusView = TextView(this).apply {
            text = "Статус: не подключено"
            setTextColor(Color.parseColor("#B00020"))
        }
        controls.addView(statusView)

        // --- 2. Проверка адаптера ---
        controls.addView(header("2. Проверка адаптера (перед работой с блоками!)"))
        controls.addView(buttonRow(
            "Проверить адаптер" to { runAdapterCheck() },
            "Остановить" to { cancelled.set(true) }
        ))

        // --- 3. Двигатель OBD-II ---
        controls.addView(header("3. Двигатель (OBD-II)"))
        controls.addView(buttonRow(
            "Инициализация" to { runOp { EngineObd(it, ::log).init() } },
            "Читать ошибки" to { runOp { EngineObd(it, ::log).readDtc() } }
        ))
        controls.addView(buttonRow(
            "Сбросить ошибки" to { confirmClearDtc() },
            "Живые параметры" to { runOp { EngineObd(it, ::log).readLive() } }
        ))

        // --- 4. Автоскан модулей ---
        controls.addView(header("4. Автоскан модулей (read-only)"))
        controls.addView(small("Только чтение: 81 / 1A / 18 / 13. Зажигание — в положение ON."))
        controls.addView(buttonRow(
            "Скан по кандидатам" to { runScan(full = false) },
            "Полный свип 01–FF" to { runScan(full = true) },
            "Остановить скан" to { cancelled.set(true) }
        ))

        // --- 5. Ручной KWP-режим ---
        controls.addView(header("5. Ручной KWP-режим"))
        controls.addView(small(
            "Кандидаты ABS (НЕ проверены, предположительно): заголовок 80 28 F1 / 80 29 F1 / 80 2B F1, " +
            "команда 81, затем 1A 9B или 18 00 FF 00."
        ))
        headerEdit = EditText(this).apply {
            hint = "Заголовок ATSH (напр. 80 28 F1)"
            setText("80 28 F1")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        controls.addView(headerEdit)
        cmdEdit = EditText(this).apply {
            hint = "Команда (напр. 81 или 18 00 FF 00)"
            setText("81")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        controls.addView(cmdEdit)
        protoSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("ATSP5 — KWP fast", "ATSP4 — KWP 5-baud", "ATSP3 — ISO 9141-2",
                    "ATSP6 — CAN 11/500", "ATSP0 — авто")
            )
        }
        controls.addView(protoSpinner)
        controls.addView(buttonRow(
            "Заголовок + команда" to { manualSend(withHeader = true) },
            "Сырая команда" to { manualSend(withHeader = false) }
        ))
        keepAliveBox = CheckBox(this).apply {
            text = "Keep-alive 3E (держать сессию KWP)"
        }
        controls.addView(keepAliveBox)

        // --- 6. Лог ---
        controls.addView(header("6. Лог"))
        controls.addView(buttonRow(
            "Очистить лог" to { clearLog() },
            "Экспорт лога" to { exportLog() }
        ))

        val controlsScroll = ScrollView(this).apply {
            addView(controls)
        }
        root.addView(controlsScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.1f))

        logView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
            setBackgroundColor(Color.parseColor("#111111"))
            setTextColor(Color.parseColor("#9CDC7E"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        logScroll = ScrollView(this).apply { addView(logView) }
        root.addView(logScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f))

        return root
    }

    private fun header(text: String) = TextView(this).apply {
        this.text = text
        setTypeface(null, Typeface.BOLD)
        textSize = 15f
        setPadding(0, dp(10), 0, dp(2))
    }

    private fun small(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.DKGRAY)
    }

    private fun buttonRow(vararg items: Pair<String, () -> Unit>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for ((label, action) in items) {
            val b = Button(this).apply {
                text = label
                textSize = 12f
                isAllCaps = false
                setOnClickListener { action() }
            }
            row.addView(b, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ================= Лог =================

    private fun log(msg: String) {
        ui.post {
            val line = "[${timeFmt.format(Date())}] $msg\n"
            logBuffer.append(line)
            logView.append(line)
            logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun clearLog() {
        logBuffer.setLength(0)
        logView.text = ""
    }

    private fun exportLog() {
        try {
            val name = "elm_log_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"
            val dir = getExternalFilesDir(null) ?: filesDir
            val f = File(dir, name)
            f.writeText(logBuffer.toString())
            log("Лог сохранён: ${f.absolutePath}")
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, name)
                putExtra(Intent.EXTRA_TEXT, logBuffer.toString())
            }
            startActivity(Intent.createChooser(send, "Экспорт лога"))
        } catch (e: Exception) {
            log("Ошибка экспорта: ${e.message}")
        }
    }

    // ================= Разрешения =================

    private fun hasBtPermission(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN), REQ_BT)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_BT) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                refreshDevices()
            } else {
                log("Нет разрешения Bluetooth — работа невозможна. Выдайте его в настройках.")
            }
        }
    }

    // ================= Подключение =================

    @SuppressLint("MissingPermission")
    private fun refreshDevices() {
        val adapter = btAdapter
        if (adapter == null) { log("Bluetooth не поддерживается этим устройством."); return }
        if (!adapter.isEnabled) { log("Bluetooth выключен — включите его в настройках."); return }
        if (!hasBtPermission()) { requestBtPermission(); return }
        try {
            devices = adapter.bondedDevices.toList()
        } catch (e: SecurityException) {
            log("SecurityException: ${e.message}"); return
        }
        if (devices.isEmpty()) {
            log("Список сопряжённых устройств пуст. Сопрягите ELM327 в настройках Bluetooth (PIN 1234/0000).")
        }
        deviceSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            devices.map { "${it.name ?: "?"} (${it.address})" }
        )
        log("Найдено сопряжённых устройств: ${devices.size}")
    }

    @SuppressLint("MissingPermission")
    private fun connect() {
        val adapter = btAdapter ?: run { log("Bluetooth не поддерживается."); return }
        if (!adapter.isEnabled) { log("Bluetooth выключен."); return }
        if (!hasBtPermission()) { requestBtPermission(); return }
        val idx = deviceSpinner.selectedItemPosition
        if (idx < 0 || idx >= devices.size) { log("Выберите устройство из списка."); return }
        if (elm != null) { log("Уже подключено — сначала отключитесь."); return }
        val dev = devices[idx]
        log("Подключение к ${dev.name} (${dev.address})…")
        executor.execute {
            busy.set(true)
            try {
                val s = dev.createRfcommSocketToServiceRecord(SPP_UUID)
                try { adapter.cancelDiscovery() } catch (_: SecurityException) { }
                s.connect()
                socket = s
                val e = Elm327(s)
                elm = e
                log("Сокет открыт. Инициализация ELM327…")
                for (c in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH1", "ATSP0")) {
                    val r = e.send(c, if (c == "ATZ") 8000 else 3000)
                    log("$c → $r")
                }
                setStatus(true, dev.name ?: dev.address)
                log("Готово. Рекомендуется сначала «Проверить адаптер».")
            } catch (e: IOException) {
                log("Ошибка подключения: ${e.message}. Проверьте, что адаптер включён и в зоне действия.")
                closeSocket()
            } catch (e: SecurityException) {
                log("SecurityException: ${e.message}")
                closeSocket()
            } finally {
                busy.set(false)
            }
        }
    }

    private fun disconnect() {
        cancelled.set(true)
        executor.execute {
            disconnectInternal()
            log("Отключено.")
        }
    }

    private fun disconnectInternal() {
        elm?.close()
        elm = null
        closeSocket()
        setStatus(false, "")
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: IOException) { }
        socket = null
        if (elm != null) { elm = null }
        setStatus(false, "")
    }

    private fun setStatus(connected: Boolean, name: String) {
        ui.post {
            statusView.text = if (connected) "Статус: подключено ($name)" else "Статус: не подключено"
            statusView.setTextColor(
                Color.parseColor(if (connected) "#0B7A4B" else "#B00020"))
        }
    }

    // ================= Операции =================

    /** Выполнить операцию с ELM в рабочем потоке (с защитой от параллелизма). */
    private fun runOp(op: (Elm327) -> Unit) {
        val e = elm ?: run { log("Сначала подключитесь к адаптеру."); return }
        cancelled.set(false)
        executor.execute {
            busy.set(true)
            try {
                op(e)
            } catch (ex: IOException) {
                log("Обрыв связи: ${ex.message}")
                disconnectInternal()
            } catch (ex: Exception) {
                log("Ошибка: ${ex.message}")
            } finally {
                busy.set(false)
            }
        }
    }

    private fun runAdapterCheck() {
        runOp { e ->
            AdapterChecker(e, ::log) { cancelled.get() }.run()
        }
    }

    private fun runScan(full: Boolean) {
        runOp { e ->
            val scanner = ModuleScanner(e, ::log) { cancelled.get() }
            if (full) scanner.scanFull() else scanner.scanCandidates()
        }
    }

    private fun confirmClearDtc() {
        if (elm == null) { log("Сначала подключитесь к адаптеру."); return }
        AlertDialog.Builder(this)
            .setTitle("Сбросить ошибки двигателя?")
            .setMessage(
                "Mode 04 сотрёт сохранённые коды И сбросит мониторы готовности. " +
                "Не делайте этого перед техосмотром/проверкой выбросов. Продолжить?"
            )
            .setPositiveButton("Сбросить") { _, _ ->
                runOp { EngineObd(it, ::log).clearDtc() }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun manualSend(withHeader: Boolean) {
        val hdr = headerEdit.text.toString().trim()
        val cmd = cmdEdit.text.toString().trim()
        if (cmd.isEmpty()) { log("Введите команду."); return }
        val proto = listOf("5", "4", "3", "6", "0")[protoSpinner.selectedItemPosition]
        runOp { e ->
            log("=== РУЧНОЙ РЕЖИМ ===")
            if (withHeader) {
                for (c in listOf("ATE0", "ATL0", "ATH1", "ATCAF0", "ATSP$proto")) {
                    e.send(c, 2000)
                }
                if (hdr.isNotEmpty()) {
                    val r = e.send("ATSH $hdr", 3000)
                    log("ATSH $hdr → $r")
                }
            }
            val r = e.send(cmd, 12000)
            log("$cmd → ${if (r.isEmpty()) "(нет ответа, таймаут)" else r}")
        }
    }

    // ================= Keep-alive =================

    /**
     * KWP рвёт сессию после ~5 с бездействия (P3max) — при включённой галке
     * шлём TesterPresent (3E), пока адаптер свободен.
     */
    private fun startKeepAliveLoop() {
        ui.postDelayed(object : Runnable {
            override fun run() {
                val e = elm
                if (keepAliveBox.isChecked && e != null && !busy.get()) {
                    executor.execute {
                        if (!busy.get()) {
                            busy.set(true)
                            try { e.send("3E", 2000) } catch (_: Exception) { }
                            finally { busy.set(false) }
                        }
                    }
                }
                ui.postDelayed(this, 2500)
            }
        }, 2500)
    }
}
