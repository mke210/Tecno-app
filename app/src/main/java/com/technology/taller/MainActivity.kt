package com.technology.taller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.technology.taller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val titulos = listOf("📝 Nuevo", "📋 Historial", "⚙️ Config")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseHelper.asegurarSesion(
            onListo = {},
            onError = { Toast.makeText(this, "Error de conexión: ${it.message}", Toast.LENGTH_LONG).show() }
        )

        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titulos[position]
        }.attach()

        pedirPermisos()
    }

    private fun pedirPermisos() {
        val permisos = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisos.add(Manifest.permission.BLUETOOTH_CONNECT)
            permisos.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        permisos.add(Manifest.permission.CAMERA)
        val faltantes = permisos.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, faltantes.toTypedArray(), 100)
        }
    }

    /** Cambia a una pestaña específica desde cualquier fragmento (ej. ir a Config > Impresora). */
    fun irAPestania(index: Int) {
        binding.viewPager.currentItem = index
    }
}
