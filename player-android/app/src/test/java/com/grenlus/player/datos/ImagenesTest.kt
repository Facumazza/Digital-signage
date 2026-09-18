package com.grenlus.player.datos

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagenesTest {

    @Test
    fun `una foto de camara en una TV 1080p se reduce a la mitad`() =
        // 4000x3000 -> 2000x1500: sigue cubriendo 1920x1080 y ocupa 4 veces menos.
        assertEquals(2, Imagenes.factorDeReduccion(4000, 3000, 1920, 1080))

    @Test
    fun `una imagen 8K en una TV 1080p se reduce a la cuarta parte`() =
        assertEquals(4, Imagenes.factorDeReduccion(7680, 4320, 1920, 1080))

    @Test
    fun `una imagen del tamanio de la pantalla no se toca`() =
        assertEquals(1, Imagenes.factorDeReduccion(1920, 1080, 1920, 1080))

    @Test
    fun `una imagen mas chica que la pantalla no se toca`() =
        assertEquals(1, Imagenes.factorDeReduccion(800, 600, 1920, 1080))

    @Test
    fun `nunca queda por debajo de la pantalla en ninguna dimension`() =
        // 3840 de ancho alcanzaria para /2, pero 1500 de alto quedaria en 750.
        assertEquals(1, Imagenes.factorDeReduccion(3840, 1500, 1920, 1080))

    @Test
    fun `una pantalla sin medidas no reduce`() =
        assertEquals(1, Imagenes.factorDeReduccion(4000, 3000, 0, 0))
}
