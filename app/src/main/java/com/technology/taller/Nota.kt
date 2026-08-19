package com.technology.taller

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Representa una Nota de Remisión de servicio técnico, con todos los campos
 * de la versión web (Technology_Zaachila.html).
 *
 * IMPORTANTE: los nombres de campo en Firestore se dejan en snake_case
 * (@PropertyName) para que coincidan EXACTAMENTE con los que escribe el
 * HTML. Así, si usas el mismo proyecto de Firebase en la web y en esta app,
 * ambas leen y editan las mismas remisiones en tiempo real.
 */
data class Nota(
    @get:Exclude @set:Exclude
    var id: String = "",

    var folio: String = "",
    var cliente: String = "",
    var telefono: String = "",
    var direccion: String = "",

    @get:PropertyName("equipo_recibido") @set:PropertyName("equipo_recibido")
    var equipoRecibido: String = "Laptop",

    var marca: String = "",

    @get:PropertyName("tipo_servicio") @set:PropertyName("tipo_servicio")
    var tipoServicio: String = TiposReparacion.FORMATEO,

    var fallas: String = "",
    var anotaciones: String = "",

    @get:PropertyName("condiciones_equipo") @set:PropertyName("condiciones_equipo")
    var condicionesEquipo: String = "",

    @get:PropertyName("cargo_cargador") @set:PropertyName("cargo_cargador")
    var cargoCargador: Boolean = false,

    @get:PropertyName("dejo_teclado") @set:PropertyName("dejo_teclado")
    var dejoTeclado: Boolean = false,

    @get:PropertyName("dejo_mouse") @set:PropertyName("dejo_mouse")
    var dejoMouse: Boolean = false,

    @get:PropertyName("dejo_cable") @set:PropertyName("dejo_cable")
    var dejoCable: Boolean = false,

    @get:PropertyName("otros_accesorios") @set:PropertyName("otros_accesorios")
    var otrosAccesorios: String = "",

    var refacciones: MutableList<Refaccion> = mutableListOf(),

    @get:PropertyName("costo_inicial") @set:PropertyName("costo_inicial")
    var costoInicial: Double = 0.0,

    var anticipo: Double = 0.0,

    @get:PropertyName("precio_total") @set:PropertyName("precio_total")
    var precioTotal: Double = 0.0,

    var fotos: MutableList<String> = mutableListOf(), // imágenes en base64 (comprimidas)
    var fecha: String = "",

    @get:PropertyName("fecha_entrega") @set:PropertyName("fecha_entrega")
    var fechaEntrega: String = "",

    var negocio: String = "Technology Zaachila - Guelache 301",

    @get:PropertyName("celular_taller") @set:PropertyName("celular_taller")
    var celularTaller: String = "9513945266",

    // Solo lo usa la app (para poder ordenar el historial de forma confiable);
    // no rompe nada si un documento creado desde la web no lo trae.
    var timestamp: Long = System.currentTimeMillis()
) : Serializable {

    @get:Exclude
    val saldoPendiente: Double
        get() = (precioTotal - anticipo).coerceAtLeast(0.0)

    @get:Exclude
    val totalRefacciones: Double
        get() = refacciones.sumOf { it.costo }

    @get:Exclude
    val accesoriosTexto: String
        get() {
            val items = mutableListOf<String>()
            if (cargoCargador) items.add("Cargador")
            if (dejoTeclado) items.add("Teclado")
            if (dejoMouse) items.add("Mouse")
            if (dejoCable) items.add("Cable de corriente")
            if (otrosAccesorios.isNotBlank()) items.add(otrosAccesorios.trim())
            return if (items.isEmpty()) "Solo equipo (sin accesorios)" else items.joinToString(", ")
        }
}

data class Refaccion(
    var nombre: String = "",
    var costo: Double = 0.0
) : Serializable

object TiposReparacion {
    const val FORMATEO = "Formateo de Sistema"
    const val CAMBIO_PIEZA = "Cambio de Pieza"
    const val MANTENIMIENTO = "Mantenimiento Preventivo"
    const val REVISION_GENERAL = "Revisión General"
    const val DIAGNOSTICO = "Diagnóstico General"

    val lista = listOf(FORMATEO, CAMBIO_PIEZA, MANTENIMIENTO, REVISION_GENERAL, DIAGNOSTICO)
}

object TiposEquipo {
    val lista = listOf("Laptop", "PC", "All in One", "Celular", "Tableta", "Disco Duro", "Monitor", "Otro")
}
