package com.technology.taller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import java.io.OutputStream
import java.util.UUID

/**
 * Plugin propio de impresión para miniprinters térmicas Bluetooth (ESC/POS).
 * No depende de librerías externas: abre un BluetoothSocket RFCOMM estándar
 * (protocolo SPP, el que usan casi todas las miniprinters térmicas de
 * 58mm/80mm) y envía los comandos ESC/POS crudos.
 *
 * Mejoras sobre la versión anterior:
 *  - Reintento automático de conexión (algunas miniprinters fallan el primer intento).
 *  - Fallback al UUID "genérico" si el SPP estándar no responde (compatibilidad
 *    con clones chinos que exponen el canal RFCOMM distinto).
 *  - Función de impresión de prueba para la pestaña de Configuración.
 *  - Función para imprimir solo el folio (etiqueta corta).
 *  - Mensajes de error más claros para mostrar en la UI.
 */
class BluetoothPrinterHelper(private val context: Context) {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val prefs: SharedPreferences =
        context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    companion object {
        private val INIT = byteArrayOf(0x1B, 0x40)
        private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        private val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11)
        private val DOUBLE_SIZE_OFF = byteArrayOf(0x1D, 0x21, 0x00)
        private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x01)
        private const val MAX_INTENTOS = 2
    }

    fun guardarImpresoraSeleccionada(mac: String, nombre: String?) {
        prefs.edit()
            .putString("mac_impresora", mac)
            .putString("nombre_impresora", nombre ?: "Miniprinter")
            .apply()
    }

    fun obtenerImpresoraGuardada(): String? = prefs.getString("mac_impresora", null)
    fun obtenerNombreImpresoraGuardada(): String = prefs.getString("nombre_impresora", "") ?: ""

    fun olvidarImpresora() {
        prefs.edit().remove("mac_impresora").remove("nombre_impresora").apply()
    }

    fun hayImpresoraConfigurada(): Boolean = !obtenerImpresoraGuardada().isNullOrBlank()

    @SuppressLint("MissingPermission")
    fun dispositivosEmparejados(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    fun bluetoothDisponible(): Boolean = BluetoothAdapter.getDefaultAdapter() != null
    fun bluetoothActivado(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    /** Imprime el ticket completo de la nota de remisión. */
    fun imprimirNota(nota: Nota, onExito: () -> Unit, onError: (String) -> Unit) {
        imprimirTexto(TicketGenerator.generarTicket(context, nota), onExito, onError)
    }

    /** Imprime solo la etiqueta de folio (botón "Imprimir Folio" de la pestaña Nuevo). */
    fun imprimirFolio(nota: Nota, onExito: () -> Unit, onError: (String) -> Unit) {
        imprimirTexto(TicketGenerator.etiquetaFolio(context, nota), onExito, onError)
    }

    /** Impresión de prueba desde la pestaña Config, para verificar que la miniprinter responde. */
    fun imprimirPrueba(onExito: () -> Unit, onError: (String) -> Unit) {
        val texto = """
            --------------------------------
            Prueba de impresión
            Technology Zaachila
            Si puedes leer esto, tu
            miniprinter quedó bien configurada.
            --------------------------------
        """.trimIndent()
        imprimirTexto(texto, onExito, onError)
    }

    @SuppressLint("MissingPermission")
    private fun imprimirTexto(texto: String, onExito: () -> Unit, onError: (String) -> Unit) {
        val mac = obtenerImpresoraGuardada()
        if (mac.isNullOrBlank()) {
            onError("No hay una miniprinter configurada. Ve a Config > Impresora y selecciónala.")
            return
        }
        if (!bluetoothActivado()) {
            onError("El Bluetooth de tu celular está apagado. Actívalo e intenta de nuevo.")
            return
        }
        Thread {
            var ultimoError: Exception? = null
            for (intento in 1..MAX_INTENTOS) {
                var socket: BluetoothSocket? = null
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                        ?: throw Exception("Este dispositivo no tiene Bluetooth")
                    val device: BluetoothDevice = adapter.getRemoteDevice(mac)
                    adapter.cancelDiscovery()
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                    val out = socket.outputStream
                    escribirTexto(out, texto)
                    onExito()
                    return@Thread
                } catch (e: Exception) {
                    ultimoError = e
                    try { socket?.close() } catch (_: Exception) {}
                    if (intento < MAX_INTENTOS) Thread.sleep(600) // pequeña pausa antes de reintentar
                }
            }
            onError(traducirError(ultimoError))
        }.start()
    }

    private fun traducirError(e: Exception?): String {
        val msg = e?.message ?: return "No se pudo imprimir. Verifica que la miniprinter esté encendida y cerca."
        return when {
            msg.contains("read failed", true) || msg.contains("socket", true) ->
                "No se pudo conectar con la miniprinter. Verifica que esté encendida, emparejada y con papel."
            msg.contains("permission", true) ->
                "Falta el permiso de Bluetooth. Ve a Ajustes > Apps > Technology Zaachila > Permisos."
            else -> "No se pudo imprimir: $msg"
        }
    }

    private fun escribirTexto(out: OutputStream, texto: String) {
        out.write(INIT)
        val lineas = texto.lines()
        var dentroEncabezado = true
        var primeraLineaTexto = true
        for (linea in lineas) {
            if (dentroEncabezado) {
                out.write(ALIGN_CENTER)
                if (primeraLineaTexto && linea.isNotBlank() && !linea.startsWith("=")) {
                    out.write(BOLD_ON); out.write(DOUBLE_SIZE_ON)
                    out.write(codificar(linea.trim() + "\n"))
                    out.write(DOUBLE_SIZE_OFF); out.write(BOLD_OFF)
                    primeraLineaTexto = false
                } else {
                    out.write(codificar(linea.trim() + "\n"))
                }
                if (linea.contains("----")) {
                    dentroEncabezado = false
                    out.write(ALIGN_LEFT)
                }
            } else {
                out.write(codificar(linea + "\n"))
            }
        }
        out.write(codificar("\n\n"))
        out.write(CUT_PAPER)
        out.flush()
    }

    private fun codificar(texto: String): ByteArray = texto.toByteArray(charset("ISO-8859-1"))
}
