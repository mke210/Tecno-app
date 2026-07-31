package com.technology.taller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.technology.taller.databinding.ItemHistorialBinding
import java.text.NumberFormat
import java.util.Locale

class HistorialAdapter(
    private var notas: List<Nota>,
    private val onClick: (Nota) -> Unit,
    private val onVistaPrevia: (Nota) -> Unit,
    private val onReimprimirFolio: (Nota) -> Unit,
    private val onReimprimir: (Nota) -> Unit,
    private val onEliminar: (Nota) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.VH>() {

    private val money = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    inner class VH(val binding: ItemHistorialBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val nota = notas[position]
        with(holder.binding) {
            textCliente.text = nota.cliente
            textFolio.text = nota.folio.ifBlank { "N/A" }
            textContacto.text = "📞 ${nota.telefono}  |  ${nota.equipoRecibido} ${nota.marca}".trim()
            textServicio.text = "🛠️ ${nota.tipoServicio}  |  ⚠️ ${nota.fallas.ifBlank { "N/E" }}"
            textPrecio.text = "💰 ${money.format(nota.precioTotal)}  |  Anticipo: ${money.format(nota.anticipo)}"
            textFecha.text = nota.fecha

            if (nota.fechaEntrega.isNotBlank()) {
                textEntregado.visibility = android.view.View.VISIBLE
                textEntregado.text = "✅ Entregado: ${nota.fechaEntrega}"
            } else {
                textEntregado.visibility = android.view.View.GONE
            }

            contenedorFotosMini.removeAllViews()
            nota.fotos.take(3).forEach { base64 ->
                val img = ImageView(root.context)
                val size = (28 * root.resources.displayMetrics.density).toInt()
                val params = android.widget.LinearLayout.LayoutParams(size, size)
                params.marginEnd = 6
                img.layoutParams = params
                img.scaleType = ImageView.ScaleType.CENTER_CROP
                PhotoUtils.base64ABitmap(base64)?.let { img.setImageBitmap(it) }
                contenedorFotosMini.addView(img)
            }

            btnVistaPrevia.setOnClickListener { onVistaPrevia(nota) }
            btnReimprimirFolio.setOnClickListener { onReimprimirFolio(nota) }
            btnReimprimir.setOnClickListener { onReimprimir(nota) }
            btnEliminar.setOnClickListener { onEliminar(nota) }
            root.setOnClickListener { onClick(nota) }
        }
    }

    override fun getItemCount(): Int = notas.size

    fun actualizar(nuevas: List<Nota>) {
        notas = nuevas
        notifyDataSetChanged()
    }
}
