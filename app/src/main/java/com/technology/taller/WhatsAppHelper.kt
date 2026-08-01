package com.technology.taller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Envía el resumen del ticket al celular del cliente por WhatsApp, usando
 * un enlace wa.me con el mensaje ya escrito (el usuario solo tiene que
 * tocar "Enviar" dentro de WhatsApp).
 */
object WhatsAppHelper {

    fun enviarTicket(context: Context, nota: Nota) {
        val telefono = normalizarTelefono(nota.telefono)
        if (telefono == null) {
            Toast.makeText(
                context,
                "Agrega un teléfono válido del cliente (10 dígitos) para poder enviarle el ticket por WhatsApp.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val textoTicket = TicketGenerator.textoPlano(TicketGenerator.generarTicket(context, nota))
        val nombreNegocio = NegocioConfig(context).nombre()
        val saludo = if (nota.cliente.isNotBlank()) "Hola ${nota.cliente}" else "Hola"
        val mensaje = "$saludo, este es el resumen de tu equipo en $nombreNegocio:\n\n$textoTicket"

        try {
            val uri = Uri.parse("https://wa.me/$telefono?text=" + Uri.encode(mensaje))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Deja solo dígitos y agrega el código de país (52, México) si hacen falta. */
    private fun normalizarTelefono(telefono: String): String? {
        val soloDigitos = telefono.filter { it.isDigit() }
        if (soloDigitos.length < 10) return null
        return if (soloDigitos.length == 10) "52$soloDigitos" else soloDigitos
    }
}
