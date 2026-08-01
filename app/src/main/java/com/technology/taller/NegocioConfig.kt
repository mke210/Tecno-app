package com.technology.taller

import android.content.Context

/**
 * Guarda los datos del negocio (nombre, dirección, teléfono, lema, aviso
 * legal y mensaje de despedida) en SharedPreferences, editables desde
 * Config > Datos del negocio. Así, cualquier otro taller puede instalar
 * esta misma app y "montar" sus propios datos sin tocar el código — todo
 * el ticket impreso, la etiqueta de folio y el encabezado de la app se
 * arman con estos valores.
 */
class NegocioConfig(private val context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("negocio_config", Context.MODE_PRIVATE)

    companion object {
        private const val K_NOMBRE = "nombre"
        private const val K_DIRECCION = "direccion"
        private const val K_TELEFONO = "telefono"
        private const val K_LEMA = "lema"
        private const val K_AVISO_LEGAL = "aviso_legal"
        private const val K_MENSAJE_DESPEDIDA = "mensaje_despedida"

        const val AVISO_LEGAL_DEFECTO =
            "Si el cliente no pasa después de 3 meses por su equipo, %s no se hace responsable de la pérdida total o parcial del equipo."
        const val MENSAJE_DESPEDIDA_DEFECTO = "¡Gracias por preferirnos!"
        const val LEMA_DEFECTO = "Servicio Técnico Laptops"
    }

    fun nombre(): String = prefs.getString(K_NOMBRE, null) ?: context.getString(R.string.negocio_nombre)
    fun direccion(): String = prefs.getString(K_DIRECCION, null) ?: context.getString(R.string.negocio_direccion)
    fun telefono(): String = prefs.getString(K_TELEFONO, null) ?: context.getString(R.string.negocio_telefono)
    fun lema(): String = prefs.getString(K_LEMA, null) ?: LEMA_DEFECTO
    fun mensajeDespedida(): String = prefs.getString(K_MENSAJE_DESPEDIDA, null) ?: MENSAJE_DESPEDIDA_DEFECTO

    /** Ya con el nombre del negocio insertado. */
    fun avisoLegal(): String {
        val plantilla = prefs.getString(K_AVISO_LEGAL, null) ?: AVISO_LEGAL_DEFECTO
        return if (plantilla.contains("%s")) String.format(plantilla, nombre()) else plantilla
    }

    /** Devuelve la plantilla del aviso legal SIN reemplazar (para mostrarla editable en Config). */
    fun avisoLegalPlantilla(): String = prefs.getString(K_AVISO_LEGAL, null) ?: AVISO_LEGAL_DEFECTO

    fun guardar(
        nombre: String,
        direccion: String,
        telefono: String,
        lema: String,
        avisoLegal: String,
        mensajeDespedida: String
    ) {
        prefs.edit()
            .putString(K_NOMBRE, nombre.ifBlank { context.getString(R.string.negocio_nombre) })
            .putString(K_DIRECCION, direccion.ifBlank { context.getString(R.string.negocio_direccion) })
            .putString(K_TELEFONO, telefono.ifBlank { context.getString(R.string.negocio_telefono) })
            .putString(K_LEMA, lema.ifBlank { LEMA_DEFECTO })
            .putString(K_AVISO_LEGAL, avisoLegal.ifBlank { AVISO_LEGAL_DEFECTO })
            .putString(K_MENSAJE_DESPEDIDA, mensajeDespedida.ifBlank { MENSAJE_DESPEDIDA_DEFECTO })
            .apply()
    }

    fun restablecer() {
        prefs.edit().clear().apply()
    }
}
