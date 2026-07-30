package com.technology.taller

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.technology.taller.databinding.FragmentConfigBinding
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pestaña "Config" — incluye la sección de Impresoras (equivalente a la
 * pestaña de impresoras del HTML) ya conectada de verdad al plugin de
 * impresión Bluetooth propio, más el respaldo de datos.
 */
class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var printerHelper: BluetoothPrinterHelper

    private val lanzadorGuardarJson = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) exportarA(uri)
    }
    private val lanzadorAbrirJson = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importarDesde(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printerHelper = BluetoothPrinterHelper(requireContext())

        cargarListaImpresoras()
        actualizarEstadoImpresora()

        binding.btnActualizarDispositivos.setOnClickListener {
            cargarListaImpresoras()
            actualizarEstadoImpresora()
        }
        binding.btnAjustesBluetooth.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        binding.btnProbarImpresion.setOnClickListener { probarImpresion() }
        binding.btnOlvidarImpresora.setOnClickListener { olvidarImpresora() }

        binding.btnExportar.setOnClickListener {
            val nombre = "Technology_Backup_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())}.json"
            lanzadorGuardarJson.launch(nombre)
        }
        binding.btnImportar.setOnClickListener {
            lanzadorAbrirJson.launch(arrayOf("application/json"))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoImpresora()
    }

    @SuppressLint("MissingPermission")
    private fun cargarListaImpresoras() {
        val dispositivos = printerHelper.dispositivosEmparejados()
        binding.textSinDispositivos.visibility = if (dispositivos.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerImpresoras.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerImpresoras.adapter = DispositivoAdapter(
            dispositivos,
            printerHelper.obtenerImpresoraGuardada()
        ) { device ->
            printerHelper.guardarImpresoraSeleccionada(device.address, device.name)
            actualizarEstadoImpresora()
            Toast.makeText(requireContext(), "Impresora en uso: ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEstadoImpresora() {
        binding.textAvisoBluetooth.visibility = if (!printerHelper.bluetoothActivado()) View.VISIBLE else View.GONE
        binding.textEstadoImpresora.text = if (printerHelper.hayImpresoraConfigurada()) {
            "Impresora en uso: ${printerHelper.obtenerNombreImpresoraGuardada()}"
        } else {
            "Sin miniprinter configurada. Selecciona una de la lista de abajo."
        }
    }

    private fun probarImpresion() {
        if (!printerHelper.hayImpresoraConfigurada()) {
            Toast.makeText(requireContext(), "Primero selecciona una miniprinter de la lista.", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(requireContext(), "Enviando impresión de prueba...", Toast.LENGTH_SHORT).show()
        printerHelper.imprimirPrueba(
            onExito = { activity?.runOnUiThread { Toast.makeText(requireContext(), "✅ Impresión de prueba enviada", Toast.LENGTH_SHORT).show() } },
            onError = { msg -> activity?.runOnUiThread { Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() } }
        )
    }

    private fun olvidarImpresora() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Olvidar miniprinter")
            .setMessage("¿Quitar la miniprinter guardada? Tendrás que volver a seleccionarla.")
            .setPositiveButton("Olvidar") { _, _ ->
                printerHelper.olvidarImpresora()
                cargarListaImpresoras()
                actualizarEstadoImpresora()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportarA(uri: Uri) {
        Toast.makeText(requireContext(), "Generando respaldo...", Toast.LENGTH_SHORT).show()
        FirebaseHelper.exportarRespaldoJson(
            onListo = { json ->
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { it.write(json) }
                    }
                    Toast.makeText(requireContext(), "✅ Respaldo guardado", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "❌ Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onError = { Toast.makeText(requireContext(), "❌ Error: ${it.message}", Toast.LENGTH_LONG).show() }
        )
    }

    private fun importarDesde(uri: Uri) {
        val json = try {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
        if (json.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Importar respaldo")
            .setMessage("¿Importar las remisiones de este archivo? Se agregarán como registros nuevos.")
            .setPositiveButton("Importar") { _, _ ->
                binding.textProgresoImportacion.visibility = View.VISIBLE
                binding.textProgresoImportacion.text = "Importando..."
                FirebaseHelper.importarRespaldoJson(json,
                    onProgreso = { completados, total ->
                        activity?.runOnUiThread { binding.textProgresoImportacion.text = "Importando $completados de $total..." }
                    },
                    onListo = {
                        activity?.runOnUiThread {
                            binding.textProgresoImportacion.text = "✅ Importación completada"
                            Toast.makeText(requireContext(), "Importación completada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = {
                        activity?.runOnUiThread {
                            binding.textProgresoImportacion.text = "❌ Error: ${it.message}"
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
