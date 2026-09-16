package com.grenlus.player.datos

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * La descarga es lo que decide si una TV muestra un video entero, uno cortado
 * o nada, y no hay emulador para probarla: estos tests la ejercitan contra un
 * servidor HTTP simulado.
 */
class DescargaTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var servidor: MockWebServer
    private lateinit var api: ApiPlayer
    private lateinit var destino: File
    private lateinit var parcial: File

    /** 100 KB con bytes distintos, para que un tramo mal pegado se note. */
    private val archivo = ByteArray(100_000) { (it % 251).toByte() }

    private val contenido
        get() = ContenidoRemoto(7, "VIDEO", "/archivo", null, archivo.size.toLong())

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        api = ApiPlayer(object : Credenciales {
            override val servidor = this@DescargaTest.servidor.url("").toString().trimEnd('/')
            override val codigo = "GRN-TEST"
            override val token = "token"
        })
        destino = File(carpeta.root, "7.bin")
        parcial = File(carpeta.root, "7.bin.parcial")
    }

    @After
    fun cerrar() = servidor.shutdown()

    @Test
    fun `descarga completa sin intento previo`() {
        servidor.enqueue(respuesta(200, archivo))

        api.descargar(contenido, destino)

        assertArrayEquals(archivo, destino.readBytes())
        assertFalse(parcial.exists())
        assertNull(servidor.takeRequest().getHeader("Range"))
    }

    @Test
    fun `retoma desde donde quedo el parcial`() {
        parcial.writeBytes(archivo.copyOfRange(0, 30_000))
        servidor.enqueue(respuesta(206, archivo.copyOfRange(30_000, archivo.size)))

        api.descargar(contenido, destino)

        assertEquals("bytes=30000-", servidor.takeRequest().getHeader("Range"))
        assertArrayEquals(archivo, destino.readBytes())
        assertFalse(parcial.exists())
    }

    @Test
    fun `si el servidor no acepta retomar manda todo y se sobrescribe`() {
        parcial.writeBytes(ByteArray(30_000) { 9 })
        servidor.enqueue(respuesta(200, archivo))

        api.descargar(contenido, destino)

        assertArrayEquals(archivo, destino.readBytes())
    }

    @Test
    fun `un archivo cortado no se da por bueno y se conserva para retomar`() {
        servidor.enqueue(respuesta(200, archivo.copyOfRange(0, 60_000)))

        try {
            api.descargar(contenido, destino)
            fail("Deberia rechazar un archivo incompleto")
        } catch (e: ErrorApi) {
            assertTrue(e.message!!.contains("incompleto"))
        }

        assertFalse("No tiene que quedar con el nombre definitivo", destino.exists())
        assertEquals(60_000L, parcial.length())

        // El intento siguiente pide solo lo que falta y termina bien.
        servidor.takeRequest()
        servidor.enqueue(respuesta(206, archivo.copyOfRange(60_000, archivo.size)))
        api.descargar(contenido, destino)

        assertEquals("bytes=60000-", servidor.takeRequest().getHeader("Range"))
        assertArrayEquals(archivo, destino.readBytes())
    }

    @Test
    fun `un parcial mas grande que el archivo se descarta`() {
        parcial.writeBytes(ByteArray(150_000))
        servidor.enqueue(respuesta(200, archivo))

        api.descargar(contenido, destino)

        assertNull(servidor.takeRequest().getHeader("Range"))
        assertArrayEquals(archivo, destino.readBytes())
    }

    @Test
    fun `un parcial completo se confirma sin volver a descargar`() {
        parcial.writeBytes(archivo)

        api.descargar(contenido, destino)

        assertEquals(0, servidor.requestCount)
        assertArrayEquals(archivo, destino.readBytes())
    }

    @Test
    fun `un error del servidor no deja archivo definitivo`() {
        servidor.enqueue(MockResponse().setResponseCode(500))

        try {
            api.descargar(contenido, destino)
            fail("Deberia fallar con 500")
        } catch (e: ErrorApi) {
            // esperado
        }

        assertFalse(destino.exists())
    }

    @Test
    fun `sin tamanio del servidor descarga igual`() {
        servidor.enqueue(respuesta(200, archivo))

        api.descargar(contenido.copy(tamanoBytes = null), destino)

        assertArrayEquals(archivo, destino.readBytes())
    }

    private fun respuesta(codigo: Int, cuerpo: ByteArray) =
        MockResponse().setResponseCode(codigo).setBody(Buffer().write(cuerpo))
}
