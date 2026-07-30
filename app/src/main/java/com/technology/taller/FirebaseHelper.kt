package com.technology.taller

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

/**
 * Centraliza el acceso a Firestore: guardar, escuchar, buscar, actualizar,
 * eliminar y exportar/importar respaldo — igual que la versión web.
 */
object FirebaseHelper {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private const val COLECCION = "remisiones"

    fun asegurarSesion(onListo: () -> Unit, onError: (Exception) -> Unit) {
        val actual = auth.currentUser
        if (actual != null) {
            onListo()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { onListo() }
            .addOnFailureListener { onError(it) }
    }

    fun guardarNota(nota: Nota, onExito: (String) -> Unit, onError: (Exception) -> Unit) {
        val coleccion = db.collection(COLECCION)
        val doc = if (nota.id.isBlank()) coleccion.document() else coleccion.document(nota.id)
        nota.id = doc.id
        if (nota.folio.isBlank()) nota.folio = FolioGenerator.generar()
        doc.set(nota)
            .addOnSuccessListener { onExito(doc.id) }
            .addOnFailureListener { onError(it) }
    }

    fun actualizarNota(nota: Nota, onExito: () -> Unit, onError: (Exception) -> Unit) {
        if (nota.id.isBlank()) {
            onError(Exception("La nota no tiene id, no se puede actualizar"))
            return
        }
        db.collection(COLECCION).document(nota.id).set(nota)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { onError(it) }
    }

    /** Convierte un documento a Nota y rellena el id (excluido de la serialización). */
    private fun documentoANota(doc: com.google.firebase.firestore.DocumentSnapshot): Nota? {
        val nota = doc.toObject(Nota::class.java) ?: return null
        nota.id = doc.id
        return nota
    }

    /**
     * Escucha en vivo las últimas 30 notas, ordenadas por 'fecha' (igual que
     * cargarHistorial() en la web) en vez de 'timestamp', para que también
     * aparezcan las remisiones creadas desde el HTML.
     */
    fun escucharNotas(
        onCambio: (List<Nota>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(COLECCION)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val notas = snapshot?.documents?.mapNotNull { documentoANota(it) } ?: emptyList()
                onCambio(notas)
            }
    }

    /** Busca por nombre, teléfono o folio en todas las remisiones (igual que buscarHistorial()). */
    fun buscarNotas(query: String, onResultado: (List<Nota>) -> Unit, onError: (Exception) -> Unit) {
        val texto = query.trim().lowercase()
        if (texto.isEmpty()) {
            onResultado(emptyList())
            return
        }
        db.collection(COLECCION).get()
            .addOnSuccessListener { snapshot ->
                val encontrados = snapshot.documents.mapNotNull { documentoANota(it) }
                    .filter { nota ->
                        nota.cliente.lowercase().contains(texto) ||
                            nota.telefono.contains(texto) ||
                            nota.folio.lowercase().contains(texto)
                    }
                onResultado(encontrados)
            }
            .addOnFailureListener { onError(it) }
    }

    fun eliminarNota(id: String, onExito: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(COLECCION).document(id)
            .delete()
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { onError(it) }
    }

    /** Descarga todas las remisiones y arma el JSON de respaldo (igual que exportarJSON()). */
    fun exportarRespaldoJson(onListo: (String) -> Unit, onError: (Exception) -> Unit) {
        db.collection(COLECCION).get()
            .addOnSuccessListener { snapshot ->
                val arreglo = JSONArray()
                snapshot.documents.forEach { doc ->
                    val nota = doc.toObject(Nota::class.java) ?: return@forEach
                    arreglo.put(notaAJson(nota))
                }
                onListo(arreglo.toString(2))
            }
            .addOnFailureListener { onError(it) }
    }

    /** Importa un respaldo JSON (arreglo de remisiones) igual que importarJSON(). */
    fun importarRespaldoJson(json: String, onProgreso: (Int, Int) -> Unit, onListo: () -> Unit, onError: (Exception) -> Unit) {
        try {
            val arreglo = JSONArray(json)
            if (arreglo.length() == 0) {
                onListo()
                return
            }
            var completados = 0
            for (i in 0 until arreglo.length()) {
                val nota = jsonANota(arreglo.getJSONObject(i))
                nota.id = "" // se generan documentos nuevos, igual que .add() en la web
                guardarNota(nota,
                    onExito = {
                        completados++
                        onProgreso(completados, arreglo.length())
                        if (completados == arreglo.length()) onListo()
                    },
                    onError = { onError(it) }
                )
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun notaAJson(nota: Nota): JSONObject {
        val obj = JSONObject()
        obj.put("folio", nota.folio)
        obj.put("cliente", nota.cliente)
        obj.put("telefono", nota.telefono)
        obj.put("direccion", nota.direccion)
        obj.put("equipo_recibido", nota.equipoRecibido)
        obj.put("marca", nota.marca)
        obj.put("tipo_servicio", nota.tipoServicio)
        obj.put("fallas", nota.fallas)
        obj.put("anotaciones", nota.anotaciones)
        obj.put("cargo_cargador", nota.cargoCargador)
        obj.put("solo_equipo", nota.soloEquipo)
        obj.put("dejo_ambos", nota.dejoAmbos)
        val refs = JSONArray()
        nota.refacciones.forEach { r ->
            val ro = JSONObject(); ro.put("nombre", r.nombre); ro.put("costo", r.costo); refs.put(ro)
        }
        obj.put("refacciones", refs)
        obj.put("anticipo", nota.anticipo)
        obj.put("precio_total", nota.precioTotal)
        val fotos = JSONArray(); nota.fotos.forEach { fotos.put(it) }
        obj.put("fotos", fotos)
        obj.put("fecha", nota.fecha)
        obj.put("fecha_entrega", nota.fechaEntrega)
        return obj
    }

    private fun jsonANota(obj: JSONObject): Nota {
        val nota = Nota()
        nota.folio = obj.optString("folio", "")
        nota.cliente = obj.optString("cliente", "")
        nota.telefono = obj.optString("telefono", "")
        nota.direccion = obj.optString("direccion", "")
        nota.equipoRecibido = obj.optString("equipo_recibido", "Laptop")
        nota.marca = obj.optString("marca", "")
        nota.tipoServicio = obj.optString("tipo_servicio", TiposReparacion.FORMATEO)
        nota.fallas = obj.optString("fallas", "")
        nota.anotaciones = obj.optString("anotaciones", "")
        nota.cargoCargador = obj.optBoolean("cargo_cargador", false)
        nota.soloEquipo = obj.optBoolean("solo_equipo", false)
        nota.dejoAmbos = obj.optBoolean("dejo_ambos", false)
        val refs = obj.optJSONArray("refacciones")
        if (refs != null) {
            for (i in 0 until refs.length()) {
                val ro = refs.getJSONObject(i)
                nota.refacciones.add(Refaccion(ro.optString("nombre", ""), ro.optDouble("costo", 0.0)))
            }
        }
        nota.anticipo = obj.optDouble("anticipo", 0.0)
        nota.precioTotal = obj.optDouble("precio_total", 0.0)
        val fotos = obj.optJSONArray("fotos")
        if (fotos != null) for (i in 0 until fotos.length()) nota.fotos.add(fotos.getString(i))
        nota.fecha = obj.optString("fecha", "")
        nota.fechaEntrega = obj.optString("fecha_entrega", "")
        return nota
    }
}
