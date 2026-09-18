package com.grenlus.player.datos

import com.grenlus.player.datos.Sincronizador.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cuando la TV cambia de contenido, cuando sigue y cuando se detiene. */
class DecisionTest {

    private val video = ContenidoRemoto(1, "VIDEO", "/a", null, 10)

    private fun config(playlistId: Long?, version: Long?, vararg contenidos: ContenidoRemoto) =
        Configuracion(playlistId, version, true, contenidos.toList())

    @Test
    fun `misma playlist y misma version sigue sin tocar nada`() =
        assertEquals(Decision.SEGUIR, Sincronizador.decidir(config(5, 3, video), 5, 3))

    @Test
    fun `nueva version de la misma playlist actualiza`() =
        assertEquals(Decision.ACTUALIZAR, Sincronizador.decidir(config(5, 4, video), 5, 3))

    @Test
    fun `otra playlist con el mismo numero de version actualiza`() =
        // Antes esto daba "sin cambios" y la TV seguia con la playlist vieja.
        assertEquals(Decision.ACTUALIZAR, Sincronizador.decidir(config(8, 3, video), 5, 3))

    @Test
    fun `playlist asignada pero vacia sigue con lo que mostraba`() =
        // Antes cortaba y dejaba "Sin contenido asignado" en la TV.
        assertEquals(Decision.SEGUIR, Sincronizador.decidir(config(8, 1), 5, 3))

    @Test
    fun `sin playlist asignada se detiene`() =
        assertEquals(Decision.DETENER, Sincronizador.decidir(config(null, null), 5, 3))

    @Test
    fun `dispositivo recien configurado actualiza`() =
        assertEquals(Decision.ACTUALIZAR, Sincronizador.decidir(config(5, 1, video), -1, -1))

    @Test
    fun `dispositivo actualizado desde la version anterior del player actualiza una vez`() =
        // Tenia versionLocal guardada pero no playlistLocal: se resincroniza
        // una vez (sin volver a bajar archivos que ya estan) y queda al dia.
        assertEquals(Decision.ACTUALIZAR, Sincronizador.decidir(config(5, 3, video), -1, 3))
}
