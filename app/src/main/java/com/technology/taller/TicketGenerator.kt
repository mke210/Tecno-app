package com.technology.taller

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/**
 * Genera el texto del ticket de remisión, igual que generarTicketTexto()
 * de la versión web, incluyendo el aviso legal de 3 meses y la firma de
 * conformidad del cliente.
 *
 * Las líneas que empiezan con "@B" se imprimen en negrita a tamaño normal,
 * y las que empiezan con "@@" en negrita a doble tamaño (ver
 * BluetoothPrinterHelper.escribirTexto). Esos prefijos se quitan antes de
 * imprimir, nunca se ven en el papel.
 */
object TicketGenerator {

    fun generarTicket(context: Context, nota: Nota): String {
        val money = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val entrega = when {
            nota.dejoAmbos -> "Dejó equipo y cargador"
            nota.cargoCargador -> "Dejó cargador"
            nota.soloEquipo -> "Solo equipo (sin cargador)"
            else -> "No especificado"
        }

        val refaccionesTexto = if (nota.refacciones.isEmpty()) {
            "Ninguna"
        } else {
            nota.refacciones.joinToString("\n") { "  * ${it.nombre}: ${money.format(it.costo)}" }
        }

        val nombreNegocio = context.getString(R.string.negocio_nombre)
        val direccion = context.getString(R.string.negocio_direccion)
        val telefono = context.getString(R.string.negocio_telefono)
        val partesNombre = nombreNegocio.trim().split(" ", limit = 2)
        val primeraPalabra = partesNombre.getOrElse(0) { nombreNegocio }.uppercase()
        val segundaPalabra = partesNombre.getOrElse(1) { "" }.uppercase()

        return """
============================
@M$primeraPalabra
@M$segundaPalabra
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
Condiciones al recibir: ${nota.condicionesEquipo.ifBlank { "Sin observaciones" }}
----------------------------
Servicio: ${nota.tipoServicio}
Falla(s): ${nota.fallas.ifBlank { "N/E" }}
Anotaciones: ${nota.anotaciones.ifBlank { "Ninguna" }}
----------------------------
Entrega: $entrega
----------------------------
Refacciones / Extra:
$refaccionesTexto
----------------------------
Costo inicial: ${money.format(nota.costoInicial)}
Anticipo: ${money.format(nota.anticipo)}
COSTO FINAL: ${money.format(nota.precioTotal)}
RESTA POR PAGAR: ${money.format(nota.saldoPendiente)}
----------------------------
Fecha Ingreso: ${nota.fecha.ifBlank { "N/E" }}
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

Acepto las condiciones,
precio y tiempos descritos
en esta nota de remisión:

_____________________________
Firma de conformidad del cliente
        """.trimIndent()
    }

    fun etiquetaFolio(context: Context, nota: Nota): String {
        val nombreNegocio = context.getString(R.string.negocio_nombre)
        val direccion = context.getString(R.string.negocio_direccion)
        return """
================================
@@FOLIO: ${nota.folio}
   Cliente: ${nota.cliente.ifBlank { "Cliente" }}
   ${nombreNegocio.uppercase()}
   $direccion, Oaxaca
================================
        """.trimIndent()
    }
}
