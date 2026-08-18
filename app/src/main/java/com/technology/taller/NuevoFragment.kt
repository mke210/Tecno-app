package com.technology.taller

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.technology.taller.databinding.FragmentNuevoBinding
import com.technology.taller.databinding.ItemFotoBinding
import com.technology.taller.databinding.ItemRefaccionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NuevoFragment : Fragment() {

    private var _binding: FragmentNuevoBinding? = null
    private val binding get() = _binding!!

    private lateinit var printerHelper: BluetoothPrinterHelper
    private val refacciones = mutableListOf<Refaccion>()
    private val fotos = mutableListOf<String>() // base64
    private var folioActual = ""

    private val lanzadorCamara = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) agregarFoto(bitmap)
    }

    private val lanzadorGaleria = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            PhotoUtils.uriABitmap(requireContext(), uri)?.let { agregarFoto(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNuevoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printerHelper = BluetoothPrinterHelper(requireContext())

        binding.spinnerEquipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, TiposEquipo.lista).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerTipoServicio.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, TiposReparacion.lista).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        generarFolio()
        generarFechaIngresoPorHoy()
        renderizarPreciosRapidos()

        binding.btnImprimirFolio.setOnClickListener { imprimirFolio() }
        binding.inputFechaIngreso.setOnClickListener { mostrarSelectorFecha(binding.inputFechaIngreso) }
        binding.inputFechaEntrega.setOnClickListener { mostrarSelectorFecha(binding.inputFechaEntrega) }
        binding.btnAgregarRefaccion.setOnClickListener {
            refacciones.add(Refaccion())
            renderizarRefacciones()
        }
        binding.btnTomarFoto.setOnClickListener { lanzadorCamara.launch(null) }
        binding.btnElegirGaleria.setOnClickListener { lanzadorGaleria.launch("image/*") }

        binding.inputCostoInicial.addTextChangedListener(watcher { sugerirTotal() })
        binding.inputAnticipo.addTextChangedListener(watcher { actualizarSaldoPendiente() })
        binding.inputPrecioTotal.addTextChangedListener(watcher { actualizarSaldoPendiente() })
        sugerirTotal()

        binding.btnGuardar.setOnClickListener { guardar(previsualizar = false, imprimir = false) }
        binding.btnVistaPrevia.setOnClickListener { mostrarVistaPrevia() }
        binding.btnImprimir.setOnClickListener { guardar(previsualizar = false, imprimir = true) }
        binding.btnWhatsapp.setOnClickListener { enviarWhatsApp() }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderizarPreciosRapidos()
    }

    private fun generarFolio() {
        folioActual = FolioGenerator.generar()
        binding.inputFolio.setText(folioActual)
    }

    private fun generarFechaIngresoPorHoy() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
        binding.inputFechaIngreso.setText(sdf.format(java.util.Date()))
    }

    private fun mostrarSelectorFecha(campo: android.widget.EditText) {
        val cal = Calendar.getInstance()
        val sdfSoloFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
        val sdfHora = SimpleDateFormat("HH:mm", Locale("es", "MX"))
        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            val horaActual = if (campo == binding.inputFechaIngreso) " ${sdfHora.format(java.util.Date())}" else ""
            campo.setText(sdfSoloFecha.format(cal.time) + horaActual)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ---------- Refacciones ----------
    private fun renderizarPreciosRapidos() {
        val money = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        binding.contenedorPreciosRapidos.removeAllViews()
        val precios = PreciosConfig(requireContext()).obtenerPrecios()
        if (precios.isEmpty()) {
            binding.textPreciosRapidos.visibility = View.GONE
            return
        }
        binding.textPreciosRapidos.visibility = View.VISIBLE
        precios.forEach { precio ->
            val boton = android.widget.Button(requireContext()).apply {
                text = "${precio.nombre}\n${money.format(precio.precio)}"
                textSize = 11f
                isAllCaps = false
                setPadding(24, 8, 24, 8)
                backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.primario, null))
                setTextColor(resources.getColor(R.color.blanco, null))
            }
            val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.marginEnd = 12
            boton.layoutParams = params
            boton.setOnClickListener {
                refacciones.add(Refaccion(precio.nombre, precio.precio))
                renderizarRefacciones()
                sugerirTotal()
                Toast.makeText(requireContext(), "Agregado: ${precio.nombre}", Toast.LENGTH_SHORT).show()
            }
            binding.contenedorPreciosRapidos.addView(boton)
        }
    }

    private fun renderizarRefacciones() {
        binding.contenedorRefacciones.removeAllViews()
        refacciones.forEachIndexed { index, refaccion ->
            val item = ItemRefaccionBinding.inflate(layoutInflater, binding.contenedorRefacciones, false)
            item.inputNombreRefaccion.setText(refaccion.nombre)
            item.inputCostoRefaccion.setText(if (refaccion.costo > 0) refaccion.costo.toString() else "")
            item.inputNombreRefaccion.addTextChangedListener(watcher { refaccion.nombre = item.inputNombreRefaccion.text.toString() })
            item.inputCostoRefaccion.addTextChangedListener(watcher {
                refaccion.costo = item.inputCostoRefaccion.text.toString().toDoubleOrNull() ?: 0.0
                sugerirTotal()
            })
            item.btnQuitarRefaccion.setOnClickListener {
                refacciones.removeAt(index)
                renderizarRefacciones()
                sugerirTotal()
            }
            binding.contenedorRefacciones.addView(item.root)
        }
    }

    private fun sugerirTotal() {
        val costoInicial = binding.inputCostoInicial.text.toString().toDoubleOrNull() ?: 0.0
        val totalRefacciones = refacciones.sumOf { it.costo }
        // El costo final se sugiere como: costo inicial + piezas/servicios agregados después.
        // El campo queda habilitado para que el usuario lo ajuste manualmente si lo necesita.
        if (!binding.inputPrecioTotal.isFocused) {
            binding.inputPrecioTotal.setText(String.format(Locale.US, "%.2f", costoInicial + totalRefacciones))
        }
        actualizarSaldoPendiente()
    }

    private fun actualizarSaldoPendiente() {
        val money = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val total = binding.inputPrecioTotal.text.toString().toDoubleOrNull() ?: 0.0
        val anticipo = binding.inputAnticipo.text.toString().toDoubleOrNull() ?: 0.0
        val saldo = (total - anticipo).coerceAtLeast(0.0)
        binding.textSaldoPendiente.text = "Resta por pagar: ${money.format(saldo)}"
    }

    // ---------- Fotos ----------
    private fun agregarFoto(bitmap: Bitmap) {
        val base64 = PhotoUtils.bitmapABase64(bitmap)
        fotos.add(base64)
        renderizarFotos()
    }

    private fun renderizarFotos() {
        binding.contenedorFotos.removeAllViews()
        fotos.forEachIndexed { index, base64 ->
            val item = ItemFotoBinding.inflate(layoutInflater, binding.contenedorFotos, false)
            PhotoUtils.base64ABitmap(base64)?.let { item.imagenFoto.setImageBitmap(it) }
            item.btnQuitarFoto.setOnClickListener {
                fotos.removeAt(index)
                renderizarFotos()
            }
            binding.contenedorFotos.addView(item.root)
        }
    }

    // ---------- Construcción de la nota ----------
    private fun construirNota(): Nota? {
        val cliente = binding.inputCliente.text.toString().trim()
        val telefono = binding.inputTelefono.text.toString().trim()
        if (cliente.isEmpty() || telefono.isEmpty()) {
            mostrarAlerta("⚠️ Nombre y teléfono son obligatorios.", esError = true)
            return null
        }
        val nota = Nota()
        nota.folio = binding.inputFolio.text.toString()
        nota.cliente = cliente
        nota.telefono = telefono
        nota.direccion = binding.inputDireccion.text.toString().trim()
        nota.equipoRecibido = binding.spinnerEquipo.selectedItem?.toString() ?: "Laptop"
        nota.marca = binding.inputMarca.text.toString().trim()
        nota.tipoServicio = binding.spinnerTipoServicio.selectedItem?.toString() ?: TiposReparacion.FORMATEO
        nota.fallas = binding.inputFallas.text.toString().trim()
        nota.condicionesEquipo = binding.inputCondiciones.text.toString().trim()
        nota.anotaciones = binding.inputAnotaciones.text.toString().trim()
        nota.cargoCargador = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioCargador.id
        nota.soloEquipo = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioSoloEquipo.id
        nota.dejoAmbos = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioAmbos.id
        nota.refacciones = refacciones.toMutableList()
        nota.costoInicial = binding.inputCostoInicial.text.toString().toDoubleOrNull() ?: 0.0
        nota.anticipo = binding.inputAnticipo.text.toString().toDoubleOrNull() ?: 0.0
        nota.precioTotal = binding.inputPrecioTotal.text.toString().toDoubleOrNull() ?: 0.0
        nota.fotos = fotos.toMutableList()
        nota.fecha = binding.inputFechaIngreso.text.toString().trim()
        nota.fechaEntrega = binding.inputFechaEntrega.text.toString().trim()
        return nota
    }

    private fun guardar(previsualizar: Boolean, imprimir: Boolean) {
        val nota = construirNota() ?: return
        binding.btnGuardar.isEnabled = false
        binding.btnImprimir.isEnabled = false
        FirebaseHelper.guardarNota(nota,
            onExito = {
                mostrarAlerta("✅ Remisión guardada. Folio: ${nota.folio}", esError = false)
                if (imprimir) imprimirNota(nota)
                limpiarFormulario()
                binding.btnGuardar.isEnabled = true
                binding.btnImprimir.isEnabled = true
            },
            onError = {
                mostrarAlerta("❌ Error al guardar: ${it.message}", esError = true)
                binding.btnGuardar.isEnabled = true
                binding.btnImprimir.isEnabled = true
            }
        )
    }

    private fun limpiarFormulario() {
        binding.inputCliente.text?.clear()
        binding.inputTelefono.text?.clear()
        binding.inputDireccion.text?.clear()
        binding.inputMarca.text?.clear()
        binding.inputFallas.text?.clear()
        binding.inputAnotaciones.text?.clear()
        binding.inputCondiciones.text?.clear()
        binding.inputAnticipo.text?.clear()
        binding.inputCostoInicial.text?.clear()
        binding.inputPrecioTotal.setText("0.00")
        binding.inputFechaEntrega.text?.clear()
        binding.radioGroupEntrega.check(binding.radioSoloEquipo.id)
        refacciones.clear(); renderizarRefacciones()
        fotos.clear(); renderizarFotos()
        generarFolio()
        generarFechaIngresoPorHoy()
    }

    // ---------- Vista previa / impresión ----------
    private fun mostrarVistaPrevia() {
        val nota = construirNota() ?: return
        val texto = TicketGenerator.textoPlano(TicketGenerator.generarTicket(requireContext(), nota))
        val textView = TextView(requireContext()).apply {
            text = texto
            setPadding(32, 24, 32, 24)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
        }
        AlertDialog.Builder(requireContext())
            .setTitle("🧾 Vista previa")
            .setView(android.widget.ScrollView(requireContext()).apply { addView(textView) })
            .setPositiveButton("Imprimir") { _, _ -> imprimirNota(nota) }
            .setNeutralButton("📲 WhatsApp") { _, _ -> ofrecerEnvioWhatsApp(nota) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun ofrecerEnvioWhatsApp(nota: Nota) {
        if (nota.fotos.isEmpty()) {
            WhatsAppHelper.enviarTicket(requireContext(), nota)
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("📲 Enviar por WhatsApp")
            .setMessage("Esta remisión tiene ${nota.fotos.size} foto(s) del equipo. WhatsApp no deja mandar texto y fotos juntos automáticamente, así que son 2 pasos:\n\n1️⃣ Enviar el ticket (abre el chat del cliente)\n2️⃣ Enviar las fotos (elige el mismo chat)")
            .setPositiveButton("1️⃣ Enviar ticket") { _, _ -> WhatsAppHelper.enviarTicket(requireContext(), nota) }
            .setNeutralButton("2️⃣ Enviar fotos") { _, _ -> WhatsAppHelper.enviarFotos(requireContext(), nota.fotos) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarWhatsApp() {
        val nota = construirNota() ?: return
        ofrecerEnvioWhatsApp(nota)
    }

    private fun imprimirNota(nota: Nota) {
        if (!printerHelper.hayImpresoraConfigurada()) {
            mostrarAlerta("Configura tu miniprinter primero en la pestaña Config > Impresora.", esError = true)
            (activity as? MainActivity)?.irAPestania(2)
            return
        }
        Toast.makeText(requireContext(), "Imprimiendo...", Toast.LENGTH_SHORT).show()
        printerHelper.imprimirNota(nota,
            onExito = { activity?.runOnUiThread { Toast.makeText(requireContext(), "Impreso correctamente", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun imprimirFolio() {
        val nota = construirNota() ?: return
        if (!printerHelper.hayImpresoraConfigurada()) {
            mostrarAlerta("Configura tu miniprinter primero en la pestaña Config > Impresora.", esError = true)
            (activity as? MainActivity)?.irAPestania(2)
            return
        }
        printerHelper.imprimirFolio(nota,
            onExito = { activity?.runOnUiThread { Toast.makeText(requireContext(), "Folio impreso", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun mostrarAlerta(texto: String, esError: Boolean) {
        binding.textAlerta.text = texto
        binding.textAlerta.setBackgroundColor(if (esError) 0xFFF8D7DA.toInt() else 0xFFD4EDDA.toInt())
        binding.textAlerta.setTextColor(if (esError) 0xFF721C24.toInt() else 0xFF155724.toInt())
        binding.textAlerta.visibility = View.VISIBLE
        binding.textAlerta.postDelayed({ binding.textAlerta.visibility = View.GONE }, 5000)
    }

    private fun watcher(onChange: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange() }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
