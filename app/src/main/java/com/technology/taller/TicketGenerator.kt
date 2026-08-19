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
        val config = NegocioConfig(context)
        val entrega = nota.accesoriosTexto

        val refaccionesTexto = if (nota.refacciones.isEmpty()) {
            "Ninguna"
        } else {
            nota.refacciones.joinToString("\n") { "  * ${it.nombre}: ${money.format(it.costo)}" }
        }

        val nombreNegocio = config.nombre()
        val direccion = config.direccion()
        val telefono = config.telefono()
        val lema = config.lema()
        val partesNombre = nombreNegocio.trim().split(" ", limit = 2)
        val primeraPalabra = partesNombre.getOrElse(0) { nombreNegocio }.uppercase()
        val segundaPalabra = partesNombre.getOrElse(1) { "" }.uppercase()

        return """
============================
@M$primeraPalabra
@M$segundaPalabra
   $lema
   $direccion
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
${config.mensajeDespedida()}
============================

AVISO LEGAL: ${config.avisoLegal()}
============================

Acepto las condiciones,
precio y tiempos descritos
en esta nota de remisión:

_____________________________
Firma de conformidad del cliente
        """.trimIndent()
    }

    fun etiquetaFolio(context: Context, nota: Nota): String {
        return """
================================
@@FOLIO: ${nota.folio}
   Fecha: ${nota.fecha}
   Cliente: ${nota.cliente.ifBlank { "Cliente" }}
   Tel: ${nota.telefono.ifBlank { "N/E" }}
   Dir: ${nota.direccion.ifBlank { "N/E" }}
================================
        """.trimIndent()
    }

    /**
     * Quita los prefijos "@M"/"@B"/"@@" que solo tienen sentido para la
     * impresora térmica (indican negrita/tamaño). Se usa para mostrar el
     * ticket en pantalla (vista previa) o mandarlo por WhatsApp, donde esos
     * prefijos no deben verse.
     */
    fun textoPlano(texto: String): String {
        return texto.lines().joinToString("\n") { linea ->
            when {
                linea.startsWith("@@") -> linea.removePrefix("@@")
                linea.startsWith("@M") -> linea.removePrefix("@M")
                linea.startsWith("@B") -> linea.removePrefix("@B")
                else -> linea
            }
        }
    }
}
