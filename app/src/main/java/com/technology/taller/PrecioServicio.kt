package com.technology.taller

import java.io.Serializable

data class PrecioServicio(
    var nombre: String = "",
    var precio: Double = 0.0
) : Serializable
