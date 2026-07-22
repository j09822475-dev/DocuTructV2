package ee.getz.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Транспорт BLE (Bluetooth Low Energy) для ELM327-адаптеров без классического
 * SPP-канала (типичны для KONNWEI/Viecar «4.0 for iOS»). Реализует UART-over-GATT:
 * команда пишется в write-характеристику, ответ приходит notify-уведомлениями.
 *
 * Сначала пробуются известные профили BLE-клонов (FFF0, FFE0, Nordic UART,
 * 18F0), затем — универсальный поиск любой пары notify+write в одном сервисе.
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class Elm327Ble(
    private val context: Context,
    private val device: BluetoothDevice,
    private val log: (String) -> Unit
) : Elm327() {

    private data class Profile(val name: String, val service: UUID, val notify: UUID, val write: UUID)

    private val profiles = listOf(
        Profile("FFF0 (FFF1/FFF2)", uuid16("fff0"), uuid16("fff1"), uuid16("fff2")),
        Profile("FFE0 (FFE1)", uuid16("ffe0"), uuid16("ffe1"), uuid16("ffe1")),
        Profile(
            "Nordic UART",
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"),
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        ),
        Profile("18F0 (2AF0/2AF1)", uuid16("18f0"), uuid16("2af0"), uuid16("2af1"))
    )

    private val rx = ConcurrentLinkedQueue<Byte>()
    private var gatt: BluetoothGatt? = null
    @Volatile private var writeChar: BluetoothGattCharacteristic? = null
    @Volatile private var connected = false
    @Volatile private var failReason: String? = null
    @Volatile private var mtuPayload = 20
    private val readyLatch = CountDownLatch(1)
    private val writeGate = Semaphore(0)

    /** Подключиться и подготовить канал (блокирует до готовности или таймаута). */
    fun connect(timeoutMs: Long = 20000): Boolean {
        log("BLE: подключение GATT к ${device.address}…")
        gatt = if (Build.VERSION.SDK_INT >= 23)
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        else
            device.connectGatt(context, false, callback)
        val ok = try {
            readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS) && connected
        } catch (_: InterruptedException) { false }
        if (!ok) {
            log("BLE: не удалось (${failReason ?: "таймаут ожидания"}).")
            close()
        }
        return ok
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("BLE: соединено, запрос MTU…")
                    if (!g.requestMtu(256)) g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    failReason = "разрыв соединения (status=$status)"
                    connected = false
                    readyLatch.countDown()
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            mtuPayload = if (status == BluetoothGatt.GATT_SUCCESS) mtu - 3 else 20
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failReason = "discoverServices status=$status"
                readyLatch.countDown()
                return
            }
            var notifyC: BluetoothGattCharacteristic? = null
            var writeC: BluetoothGattCharacteristic? = null
            var profileName: String? = null

            for (p in profiles) {
                val svc = g.getService(p.service) ?: continue
                val n = svc.getCharacteristic(p.notify) ?: continue
                val w = svc.getCharacteristic(p.write) ?: continue
                notifyC = n; writeC = w; profileName = p.name
                break
            }
            if (notifyC == null || writeC == null) {
                // Универсальный поиск: любая пара notify + write в одном сервисе
                outer@ for (svc in g.services) {
                    for (n in svc.characteristics) {
                        if (n.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) continue
                        for (w in svc.characteristics) {
                            val wp = w.properties
                            if (wp and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                            ) {
                                notifyC = n; writeC = w
                                profileName = "generic ${svc.uuid.toString().take(8)}"
                                break@outer
                            }
                        }
                    }
                }
            }
            if (notifyC == null || writeC == null) {
                failReason = "не найдена пара notify/write — устройство не UART-профиля"
                log("BLE: сервисы устройства: " +
                    g.services.joinToString { it.uuid.toString().take(8) })
                readyLatch.countDown()
                return
            }

            log("BLE: найден профиль $profileName (MTU payload $mtuPayload).")
            writeChar = writeC
            g.setCharacteristicNotification(notifyC, true)
            val cccd = notifyC.getDescriptor(CCCD)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (!g.writeDescriptor(cccd)) {
                    failReason = "не удалось записать CCCD"
                    readyLatch.countDown()
                }
            } else {
                connected = true
                readyLatch.countDown()
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            connected = status == BluetoothGatt.GATT_SUCCESS
            if (!connected) failReason = "CCCD status=$status"
            readyLatch.countDown()
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int
        ) {
            writeGate.release()
        }

        // API < 33: фреймворк вызывает этот вариант
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) {
                val v = ch.value ?: return
                for (b in v) rx.add(b)
            }
        }

        // API 33+: фреймворк вызывает этот вариант
        override fun onCharacteristicChanged(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray
        ) {
            for (b in value) rx.add(b)
        }
    }

    override fun writeBytes(data: ByteArray) {
        val g = gatt ?: throw IOException("BLE не подключен")
        val w = writeChar ?: throw IOException("BLE: нет write-характеристики")
        var off = 0
        while (off < data.size) {
            val end = minOf(off + mtuPayload, data.size)
            w.value = data.copyOfRange(off, end)
            writeGate.drainPermits()
            if (!g.writeCharacteristic(w)) throw IOException("BLE: отказ записи")
            try {
                // Ждём подтверждение записи; для WRITE_NO_RESPONSE колбэк может
                // не прийти на некоторых стеках — тогда просто продолжаем.
                writeGate.tryAcquire(2, TimeUnit.SECONDS)
            } catch (_: InterruptedException) { }
            off = end
        }
    }

    override fun tryReadByte(): Int {
        if (!connected && rx.isEmpty()) throw IOException("BLE: соединение потеряно")
        val b = rx.poll() ?: return -1
        return b.toInt() and 0xFF
    }

    override fun drainInput() {
        rx.clear()
    }

    override fun close() {
        connected = false
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) { }
        gatt = null
        writeChar = null
    }

    companion object {
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private fun uuid16(hex: String): UUID =
            UUID.fromString("0000$hex-0000-1000-8000-00805f9b34fb")
    }
}
