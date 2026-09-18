package com.grenlus.player.datos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Carga de imagenes al tamanio de la pantalla.
 *
 * Una foto de 4000x3000 decodificada entera ocupa 48 MB. Un TV Box de 1 o 2 GB
 * de RAM se queda sin memoria y la app se cierra, y una TV 1080p nunca muestra
 * mas de 1920x1080 de todos modos.
 */
object Imagenes {

    /** Null si el archivo no es una imagen valida o no entra en memoria. */
    fun cargarReducida(archivo: File, anchoPantalla: Int, altoPantalla: Int): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = factorDeReduccion(
                medidas.outWidth, medidas.outHeight, anchoPantalla, altoPantalla
            )
        }
        return try {
            BitmapFactory.decodeFile(archivo.path, opciones)
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Cuanto reducir, en potencias de 2 (lo que acepta BitmapFactory), sin
     * quedar nunca por debajo del tamanio de la pantalla: reducir de mas se
     * veria borroso.
     */
    fun factorDeReduccion(ancho: Int, alto: Int, anchoPantalla: Int, altoPantalla: Int): Int {
        if (anchoPantalla <= 0 || altoPantalla <= 0) return 1
        var factor = 1
        while (ancho / (factor * 2) >= anchoPantalla && alto / (factor * 2) >= altoPantalla) {
            factor *= 2
        }
        return factor
    }
}
