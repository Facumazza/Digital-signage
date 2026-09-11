package com.grenlus.player.datos

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Un item de la playlist, tal como lo manda el backend. */
data class ContenidoRemoto(
    val id: Long,
    val tipo: String,
    val url: String,
    val duracionSegundos: Int?,
)

/** La respuesta de /config: que deberia estar reproduciendo esta pantalla. */
data class Configuracion(
    val playlistId: Long?,
    val playlistVersion: Long?,
    val contenidos: List<ContenidoRemoto>,
) {
    val vacia: Boolean get() = playlistVersion == null || contenidos.isEmpty()
}

class ErrorApi(mensaje: String) : IOException(mensaje)

/**
 * Los dos endpoints del backend, mas la descarga de archivos.
 *
 * Se autentica con el token de la pantalla en el header X-Pantalla-Token: el
 * player no puede hacer login con usuario y contrasenia como el panel.
 */
class ApiPlayer(private val identidad: Identidad) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Los videos pueden ser grandes y la conexion de un local, mala.
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun obtenerConfiguracion(): Configuracion {
        val cuerpo = pedir("/api/player/${identidad.codigo}/config").use { respuesta ->
            if (!respuesta.isSuccessful) {
                throw ErrorApi("El servidor respondio ${respuesta.code}")
            }
            respuesta.body?.string().orEmpty()
        }

        val json = JSONObject(cuerpo)
        val items = json.optJSONArray("contenidos")

        return Configuracion(
            playlistId = json.optLongOrNull("playlistId"),
            playlistVersion = json.optLongOrNull("playlistVersion"),
            contenidos = buildList {
                for (i in 0 until (items?.length() ?: 0)) {
                    val item = items!!.getJSONObject(i)
                    add(
                        ContenidoRemoto(
                            id = item.getLong("id"),
                            tipo = item.getString("tipo"),
                            url = item.getString("url"),
                            duracionSegundos = item.optIntOrNull("duracion"),
                        )
                    )
                }
            },
        )
    }

    fun enviarHeartbeat() {
        val peticion = Request.Builder()
            .url("${identidad.servidor}/api/player/${identidad.codigo}/heartbeat")
            .header(HEADER_TOKEN, identidad.token)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .build()

        http.newCall(peticion).execute().use { respuesta ->
            if (!respuesta.isSuccessful) {
                throw ErrorApi("Heartbeat rechazado: ${respuesta.code}")
            }
        }
    }

    /**
     * Baja el archivo a un temporal y recien al final lo renombra.
     *
     * Si se corta la luz a mitad de la descarga queda un .parcial, no un
     * archivo incompleto con el nombre definitivo que el player creeria bueno.
     */
    fun descargar(contenido: ContenidoRemoto, destino: File) {
        val parcial = File(destino.parentFile, "${destino.name}.parcial")

        pedir(contenido.url).use { respuesta ->
            if (!respuesta.isSuccessful) {
                throw ErrorApi("No se pudo descargar ${contenido.id}: ${respuesta.code}")
            }
            val cuerpo = respuesta.body ?: throw ErrorApi("Respuesta vacia")
            parcial.outputStream().use { salida -> cuerpo.byteStream().copyTo(salida) }
        }

        if (destino.exists()) destino.delete()
        if (!parcial.renameTo(destino)) {
            throw ErrorApi("No se pudo guardar ${destino.name}")
        }
    }

    private fun pedir(ruta: String) = http.newCall(
        Request.Builder()
            .url("${identidad.servidor}$ruta")
            .header(HEADER_TOKEN, identidad.token)
            .build()
    ).execute()

    private companion object {
        const val HEADER_TOKEN = "X-Pantalla-Token"
    }
}

/** JSONObject devuelve 0 para un campo null; aca hace falta distinguirlos. */
private fun JSONObject.optLongOrNull(clave: String): Long? =
    if (isNull(clave)) null else optLong(clave)

private fun JSONObject.optIntOrNull(clave: String): Int? =
    if (isNull(clave)) null else optInt(clave)
