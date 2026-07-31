package com.technology.taller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.technology.taller.databinding.FragmentHistorialBinding

class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistorialAdapter
    private lateinit var printerHelper: BluetoothPrinterHelper
    private var listener: ListenerRegistration? = null
    private var buscando = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printerHelper = BluetoothPrinterHelper(requireContext())

        adapter = HistorialAdapter(
            notas = emptyList(),
            onClick = { nota -> abrirEdicion(nota) },
            onVistaPrevia = { nota -> mostrarVistaPrevia(nota) },
            onReimprimirFolio = { nota -> reimprimirFolio(nota) },
            onReimprimir = { nota -> reimprimir(nota) },
            onEliminar = { nota -> confirmarEliminar(nota) }
        )
        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistorial.adapter = adapter

        binding.btnActualizar.setOnClickListener {
            binding.inputBuscar.text?.clear()
            buscando = false
            escucharHistorial()
        }
        binding.btnBuscar.setOnClickListener { buscar() }

        binding.swipeRefresh.setOnRefreshListener {
            if (buscando) buscar() else escucharHistorial()
            binding.swipeRefresh.isRefreshing = false
        }

        escucharHistorial()
    }

    private fun escucharHistorial() {
        listener?.remove()
        listener = FirebaseHelper.escucharNotas(
            onCambio = { notas ->
                adapter.actualizar(notas)
                mostrarVacioSiAplica(notas.isEmpty())
            },
            onError = { Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show() }
        )
    }

    private fun buscar() {
        val texto = binding.inputBuscar.text.toString().trim()
        if (texto.isEmpty()) {
            buscando = false
            escucharHistorial()
            return
        }
        buscando = true
        listener?.remove()
        FirebaseHelper.buscarNotas(texto,
            onResultado = { notas ->
                adapter.actualizar(notas)
                mostrarVacioSiAplica(notas.isEmpty())
            },
            onError = { Toast.makeText(requireContext(), "Error al buscar: ${it.message}", Toast.LENGTH_LONG).show() }
        )
    }

    private fun mostrarVacioSiAplica(vacio: Boolean) {
        binding.textVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.recyclerHistorial.visibility = if (vacio) View.GONE else View.VISIBLE
    }

    private fun abrirEdicion(nota: Nota) {
        val intent = Intent(requireContext(), EditarNotaActivity::class.java)
        intent.putExtra("nota", nota)
        startActivity(intent)
    }

    private fun mostrarVistaPrevia(nota: Nota) {
        val texto = TicketGenerator.generarTicket(requireContext(), nota)
        val textView = TextView(requireContext()).apply {
            text = texto
            setPadding(32, 24, 32, 24)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
        }
        AlertDialog.Builder(requireContext())
            .setTitle("🧾 Vista previa — Folio ${nota.folio}")
            .setView(ScrollView(requireContext()).apply { addView(textView) })
            .setPositiveButton("Imprimir") { _, _ -> reimprimir(nota) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun reimprimirFolio(nota: Nota) {
        if (!printerHelper.hayImpresoraConfigurada()) {
            Toast.makeText(requireContext(), "Configura tu miniprinter en Config > Impresora", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.irAPestania(2)
            return
        }
        Toast.makeText(requireContext(), "Imprimiendo folio...", Toast.LENGTH_SHORT).show()
        printerHelper.imprimirFolio(nota,
            onExito = { activity?.runOnUiThread { Toast.makeText(requireContext(), "Folio impreso", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun reimprimir(nota: Nota) {
        if (!printerHelper.hayImpresoraConfigurada()) {
            Toast.makeText(requireContext(), "Configura tu miniprinter en Config > Impresora", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.irAPestania(2)
            return
        }
        Toast.makeText(requireContext(), "Imprimiendo...", Toast.LENGTH_SHORT).show()
        printerHelper.imprimirNota(nota,
            onExito = { activity?.runOnUiThread { Toast.makeText(requireContext(), "Impreso", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun confirmarEliminar(nota: Nota) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar remisión")
            .setMessage("¿Eliminar la remisión de ${nota.cliente} (folio ${nota.folio})? No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                FirebaseHelper.eliminarNota(nota.id,
                    onExito = { Toast.makeText(requireContext(), "Eliminada", Toast.LENGTH_SHORT).show() },
                    onError = { Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
