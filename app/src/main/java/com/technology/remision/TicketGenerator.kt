package com.technology.remision

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/**
 * Genera el texto del ticket de remisión, igual que generarTicketTexto()
 * de la versión web, incluyendo el aviso legal de 3 meses.
 */
object TicketGenerator {

    fun generarTicket(context: Context, nota: Nota): String {
        val money = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val cargador = if (nota.cargoCargador) "Sí" else "No"
        val solo = if (nota.soloEquipo) "Sí (Solo equipo)" else "No (Con cargador)"
        val ambos = if (nota.dejoAmbos) "Sí (Ambos)" else "No"

        val refaccionesTexto = if (nota.refacciones.isEmpty()) {
            "Ninguna"
        } else {
            nota.refacciones.joinToString("\n") { "  * ${it.nombre}: ${money.format(it.costo)}" }
        }

        val nombreNegocio = context.getString(R.string.negocio_nombre)
        val direccion = context.getString(R.string.negocio_direccion)
        val telefono = context.getString(R.string.negocio_telefono)

        return """
============================
   ${espaciar(nombreNegocio.uppercase())}
   Servicio Técnico Laptops
   $direccion, Oaxaca
   Cel: $telefono
============================
Folio: ${nota.folio.ifBlank { "N/A" }}
Cliente: ${nota.cliente}
Teléfono: ${nota.telefono.ifBlank { "Sin teléfono" }}
Dirección: ${nota.direccion.ifBlank { "N/E" }}
----------------------------
Equipo: ${nota.equipoRecibido.ifBlank { "N/E" }}
Marca: ${nota.marca.ifBlank { "N/E" }}
----------------------------
Servicio: ${nota.tipoServicio}
Falla(s): ${nota.fallas.ifBlank { "N/E" }}
Anotaciones: ${nota.anotaciones.ifBlank { "Ninguna" }}
----------------------------
Cargador: $cargador
Equipo: $solo
Dejó ambos: $ambos
----------------------------
Refacciones / Extra:
$refaccionesTexto
----------------------------
Anticipo: ${money.format(nota.anticipo)}
TOTAL FINAL: ${money.format(nota.precioTotal)}
----------------------------
Fecha Ingreso: ${nota.fecha}
Fecha Entrega: ${nota.fechaEntrega.ifBlank { "Pendiente" }}
============================
¡Gracias por preferirnos!
============================

AVISO LEGAL: Si el cliente no
pasa después de 3 meses por su
equipo, $nombreNegocio no se hace
responsable de la pérdida total
o parcial del equipo.
============================
        """.trimIndent()
    }

    fun etiquetaFolio(context: Context, nota: Nota): String {
        val nombreNegocio = context.getString(R.string.negocio_nombre)
        val direccion = context.getString(R.string.negocio_direccion)
        return """
================================
   FOLIO: ${nota.folio}
   Cliente: ${nota.cliente.ifBlank { "Cliente" }}
   ${nombreNegocio.uppercase()}
   $direccion, Oaxaca
================================
        """.trimIndent()
    }

    // Pequeño espaciado tipo "T E C H N O L O G Y" para el encabezado, como en la web
    private fun espaciar(texto: String): String = texto.toCharArray().joinToString(" ")
}
