package com.technology.remision

import kotlin.random.Random

/** Genera folios tipo "473aB" igual que la versión web: 3 dígitos + 2 letras. */
object FolioGenerator {
    private const val LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    fun generar(): String {
        val numeros = Random.nextInt(100, 1000)
        val letras = buildString {
            repeat(2) { append(LETRAS[Random.nextInt(LETRAS.length)]) }
        }
        return "$numeros$letras"
    }
}
