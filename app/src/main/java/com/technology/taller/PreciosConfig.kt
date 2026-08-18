package com.technology.taller

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lista de precios de servicios frecuentes (ej. "Formateo con respaldo:
 * $400"), configurable desde Config > Precios de servicios. Se usan como
 * atajos en Nuevo/Editar: con un toque agregan una pieza/servicio con el
 * nombre y costo ya llenos.
 */
class PreciosConfig(private val context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("precios_config", Context.MODE_PRIVATE)

    companion object {
        private const val K_PRECIOS = "lista_precios"

        fun listaPorDefecto() = listOf(
            PrecioServicio("Formateo de laptop con respaldo", 400.0),
            PrecioServicio("Mantenimiento preventivo", 350.0),
            PrecioServicio("Reparación de bisagras", 500.0),
            PrecioServicio("Instalación de Office", 200.0)
        )
    }

    fun obtenerPrecios(): List<PrecioServicio> {
        val json = prefs.getString(K_PRECIOS, null) ?: return listaPorDefecto()
        return try {
            val arreglo = JSONArray(json)
            (0 until arreglo.length()).map { i ->
                val obj = arreglo.getJSONObject(i)
                PrecioServicio(obj.optString("nombre", ""), obj.optDouble("precio", 0.0))
            }
        } catch (e: Exception) {
            listaPorDefecto()
        }
    }

    fun guardarPrecios(precios: List<PrecioServicio>) {
        val arreglo = JSONArray()
        precios.forEach { p ->
            val obj = JSONObject()
            obj.put("nombre", p.nombre)
            obj.put("precio", p.precio)
            arreglo.put(obj)
        }
        prefs.edit().putString(K_PRECIOS, arreglo.toString()).apply()
    }

    fun restablecer() {
        prefs.edit().remove(K_PRECIOS).apply()
    }
}
