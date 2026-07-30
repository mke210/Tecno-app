package com.technology.remision

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
import com.technology.remision.databinding.FragmentNuevoBinding
import com.technology.remision.databinding.ItemFotoBinding
import com.technology.remision.databinding.ItemRefaccionBinding
import java.text.NumberFormat
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

        binding.spinnerEquipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, TiposEquipo.lista)
        binding.spinnerTipoServicio.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, TiposReparacion.lista)

        generarFolio()

        binding.btnImprimirFolio.setOnClickListener { imprimirFolio() }
        binding.btnAgregarRefaccion.setOnClickListener {
            refacciones.add(Refaccion())
            renderizarRefacciones()
        }
        binding.btnTomarFoto.setOnClickListener { lanzadorCamara.launch(null) }
        binding.btnElegirGaleria.setOnClickListener { lanzadorGaleria.launch("image/*") }

        binding.inputAnticipo.addTextChangedListener(watcher { sugerirTotal() })
        sugerirTotal()

        binding.btnGuardar.setOnClickListener { guardar(previsualizar = false, imprimir = false) }
        binding.btnVistaPrevia.setOnClickListener { mostrarVistaPrevia() }
        binding.btnImprimir.setOnClickListener { guardar(previsualizar = false, imprimir = true) }
    }

    private fun generarFolio() {
        folioActual = FolioGenerator.generar()
        binding.inputFolio.setText(folioActual)
    }

    // ---------- Refacciones ----------
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
        val totalRefacciones = refacciones.sumOf { it.costo }
        val anticipo = binding.inputAnticipo.text.toString().toDoubleOrNull() ?: 0.0
        // Sugerencia automática (refacciones + anticipo), igual que la versión web.
        // El campo queda habilitado para que el usuario lo ajuste manualmente si lo necesita.
        if (!binding.inputPrecioTotal.isFocused) {
            binding.inputPrecioTotal.setText(String.format(Locale.US, "%.2f", totalRefacciones + anticipo))
        }
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
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
        val nota = Nota()
        nota.folio = binding.inputFolio.text.toString()
        nota.cliente = cliente
        nota.telefono = telefono
        nota.direccion = binding.inputDireccion.text.toString().trim()
        nota.equipoRecibido = binding.spinnerEquipo.selectedItem?.toString() ?: "Laptop"
        nota.marca = binding.inputMarca.text.toString().trim()
        nota.tipoServicio = binding.spinnerTipoServicio.selectedItem?.toString() ?: TiposReparacion.FORMATEO
        nota.fallas = binding.inputFallas.text.toString().trim()
        nota.anotaciones = binding.inputAnotaciones.text.toString().trim()
        nota.cargoCargador = binding.checkCargador.isChecked
        nota.soloEquipo = binding.checkSoloEquipo.isChecked
        nota.dejoAmbos = binding.checkAmbos.isChecked
        nota.refacciones = refacciones.toMutableList()
        nota.anticipo = binding.inputAnticipo.text.toString().toDoubleOrNull() ?: 0.0
        nota.precioTotal = binding.inputPrecioTotal.text.toString().toDoubleOrNull() ?: 0.0
        nota.fotos = fotos.toMutableList()
        nota.fecha = sdf.format(java.util.Date())
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
        binding.inputAnticipo.text?.clear()
        binding.inputPrecioTotal.setText("0.00")
        binding.checkCargador.isChecked = false
        binding.checkSoloEquipo.isChecked = false
        binding.checkAmbos.isChecked = false
        refacciones.clear(); renderizarRefacciones()
        fotos.clear(); renderizarFotos()
        generarFolio()
    }

    // ---------- Vista previa / impresión ----------
    private fun mostrarVistaPrevia() {
        val nota = construirNota() ?: return
        val texto = TicketGenerator.generarTicket(requireContext(), nota)
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
            .setNegativeButton("Cerrar", null)
            .show()
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
