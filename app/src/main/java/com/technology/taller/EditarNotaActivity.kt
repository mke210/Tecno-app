package com.technology.taller

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.technology.taller.databinding.ActivityEditarNotaBinding
import com.technology.taller.databinding.ItemRefaccionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditarNotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarNotaBinding
    private lateinit var printerHelper: BluetoothPrinterHelper
    private lateinit var nota: Nota
    private val refacciones = mutableListOf<Refaccion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarNotaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        printerHelper = BluetoothPrinterHelper(this)

        @Suppress("DEPRECATION")
        val notaRecibida = intent.getSerializableExtra("nota") as? Nota
        if (notaRecibida == null) {
            Toast.makeText(this, "No se pudo cargar la remisión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        nota = notaRecibida

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.spinnerEquipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, TiposEquipo.lista)
        binding.spinnerTipoServicio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, TiposReparacion.lista)

        precargar()

        binding.inputFechaEntrega.setOnClickListener { mostrarSelectorFecha() }
        binding.btnAgregarRefaccion.setOnClickListener {
            refacciones.add(Refaccion())
            renderizarRefacciones()
        }
        binding.inputCostoInicial.addTextChangedListener(watcher { sugerirTotal() })
        binding.inputAnticipo.addTextChangedListener(watcher { actualizarSaldoPendiente() })
        binding.inputPrecioTotal.addTextChangedListener(watcher { actualizarSaldoPendiente() })
        binding.btnActualizar.setOnClickListener { actualizar() }
        binding.btnReimprimir.setOnClickListener { reimprimir() }
        binding.btnEliminar.setOnClickListener { confirmarEliminar() }
    }

    private fun precargar() {
        binding.textFolioActual.text = "Folio: ${nota.folio.ifBlank { "N/A" }}"
        binding.inputCliente.setText(nota.cliente)
        binding.inputTelefono.setText(nota.telefono)
        binding.inputDireccion.setText(nota.direccion)
        binding.inputMarca.setText(nota.marca)
        binding.inputFallas.setText(nota.fallas)
        binding.inputAnotaciones.setText(nota.anotaciones)
        when {
            nota.dejoAmbos -> binding.radioGroupEntrega.check(binding.radioAmbos.id)
            nota.cargoCargador -> binding.radioGroupEntrega.check(binding.radioCargador.id)
            else -> binding.radioGroupEntrega.check(binding.radioSoloEquipo.id)
        }
        binding.inputCostoInicial.setText(if (nota.costoInicial > 0) nota.costoInicial.toString() else "")
        binding.inputAnticipo.setText(if (nota.anticipo > 0) nota.anticipo.toString() else "")
        binding.inputPrecioTotal.setText(if (nota.precioTotal > 0) nota.precioTotal.toString() else "0.00")
        binding.inputFechaEntrega.setText(nota.fechaEntrega)

        val idxEquipo = TiposEquipo.lista.indexOf(nota.equipoRecibido)
        if (idxEquipo >= 0) binding.spinnerEquipo.setSelection(idxEquipo)
        val idxServicio = TiposReparacion.lista.indexOf(nota.tipoServicio)
        if (idxServicio >= 0) binding.spinnerTipoServicio.setSelection(idxServicio)

        refacciones.clear()
        refacciones.addAll(nota.refacciones.map { it.copy() })
        renderizarRefacciones()
        actualizarSaldoPendiente()
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

    /** Sugiere el costo final = costo inicial + todas las piezas/servicios agregados (incluyendo los que se sumen después, en edición). */
    private fun sugerirTotal() {
        val costoInicial = binding.inputCostoInicial.text.toString().toDoubleOrNull() ?: 0.0
        val totalRefacciones = refacciones.sumOf { it.costo }
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

    private fun mostrarSelectorFecha() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
        android.app.DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            binding.inputFechaEntrega.setText(sdf.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun recogerCambios(): Nota {
        nota.cliente = binding.inputCliente.text.toString().trim()
        nota.telefono = binding.inputTelefono.text.toString().trim()
        nota.direccion = binding.inputDireccion.text.toString().trim()
        nota.equipoRecibido = binding.spinnerEquipo.selectedItem?.toString() ?: nota.equipoRecibido
        nota.marca = binding.inputMarca.text.toString().trim()
        nota.tipoServicio = binding.spinnerTipoServicio.selectedItem?.toString() ?: nota.tipoServicio
        nota.fallas = binding.inputFallas.text.toString().trim()
        nota.anotaciones = binding.inputAnotaciones.text.toString().trim()
        nota.cargoCargador = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioCargador.id
        nota.soloEquipo = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioSoloEquipo.id
        nota.dejoAmbos = binding.radioGroupEntrega.checkedRadioButtonId == binding.radioAmbos.id
        nota.refacciones = refacciones.toMutableList()
        nota.costoInicial = binding.inputCostoInicial.text.toString().toDoubleOrNull() ?: 0.0
        nota.anticipo = binding.inputAnticipo.text.toString().toDoubleOrNull() ?: 0.0
        nota.precioTotal = binding.inputPrecioTotal.text.toString().toDoubleOrNull() ?: 0.0
        nota.fechaEntrega = binding.inputFechaEntrega.text.toString().trim()
        return nota
    }

    private fun actualizar() {
        val actualizada = recogerCambios()
        binding.btnActualizar.isEnabled = false
        FirebaseHelper.actualizarNota(actualizada,
            onExito = {
                Toast.makeText(this, "✅ Remisión actualizada", Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = {
                binding.btnActualizar.isEnabled = true
                Toast.makeText(this, "❌ Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun reimprimir() {
        val actualizada = recogerCambios()
        if (!printerHelper.hayImpresoraConfigurada()) {
            Toast.makeText(this, "Configura tu miniprinter primero en Config > Impresora", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Imprimiendo...", Toast.LENGTH_SHORT).show()
        printerHelper.imprimirNota(actualizada,
            onExito = { runOnUiThread { Toast.makeText(this, "Impreso", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun confirmarEliminar() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar remisión")
            .setMessage("¿Eliminar definitivamente esta remisión?")
            .setPositiveButton("Eliminar") { _, _ ->
                FirebaseHelper.eliminarNota(nota.id,
                    onExito = { Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show(); finish() },
                    onError = { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun watcher(onChange: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange() }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
