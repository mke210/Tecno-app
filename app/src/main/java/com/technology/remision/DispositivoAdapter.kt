package com.technology.remision

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.technology.remision.databinding.ItemDispositivoBinding

class DispositivoAdapter(
    private val dispositivos: List<BluetoothDevice>,
    private var macSeleccionada: String?,
    private val onSeleccionado: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DispositivoAdapter.VH>() {

    inner class VH(val binding: ItemDispositivoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDispositivoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val device = dispositivos[position]
        with(holder.binding) {
            textNombreDispositivo.text = device.name ?: "Dispositivo desconocido"
            textMacDispositivo.text = device.address
            textSeleccionada.visibility = if (device.address == macSeleccionada) android.view.View.VISIBLE else android.view.View.GONE
            root.setOnClickListener {
                macSeleccionada = device.address
                notifyDataSetChanged()
                onSeleccionado(device)
            }
        }
    }

    override fun getItemCount(): Int = dispositivos.size
}
