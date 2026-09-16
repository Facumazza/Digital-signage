package com.grenlus.player.datos

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Un item de la playlist, tal como lo manda el backend. */
data class ContenidoRemoto(
    val id: Long,
    val tipo: String,
    val url: String,
    val duracionSegundos: Int?,
    /** Null si el backend es anterior a este campo: se descarga sin verificar. */
    val tamanoBytes: Long?,
)

/** La respuesta de /config: que deberia estar reproduciendo esta pantalla. */
data class Configuracion(
    val playlistId: Long?,
    val playlistVersion: Long?,
    /** false = la oficina la apago: mostrar negro, sin perder lo descargado. */
    val encendida: Boolean,
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
class ApiPlayer(private val identidad: Credenciales) {

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
            // Si falta el campo (backend anterior a esta funcion) se asume
            // encendida: preferible mostrar contenido a quedar en negro sin motivo.
            encendida = json.optBoolean("encendida", true),
            contenidos = buildList {
                for (i in 0 until (items?.length() ?: 0)) {
                    val item = items!!.getJSONObject(i)
                    add(
                        ContenidoRemoto(
                            id = item.getLong("id"),
                            tipo = item.getString("tipo"),
                            url = item.getString("url"),
                            duracionSegundos = item.optIntOrNull("duracion"),
                            tamanoBytes = item.optLongOrNull("tamanoBytes"),
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
     *
     * Si ya hay un .parcial de un intento anterior, pide solo lo que falta con
     * el header Range. Sin esto un video grande en una conexion mala empezaba
     * de cero en cada intento y podia no terminar nunca.
     *
     * Antes de renombrar compara con el tamanio que informo el servidor: un
     * corte de red no siempre tira error, y a veces solo deja el archivo corto.
     */
    fun descargar(contenido: ContenidoRemoto, destino: File) {
        val parcial = File(destino.parentFile, "${destino.name}.parcial")
        val esperado = contenido.tamanoBytes

        // Un parcial mas grande que el archivo no es retomable: viene de otra
        // version del contenido o quedo mal. Se descarta.
        if (esperado != null && parcial.length() > esperado) parcial.delete()

        val desde = if (parcial.exists()) parcial.length() else 0L

        if (esperado != null && desde == esperado) {
            // El intento anterior termino de bajar pero no llego a renombrar.
            confirmar(parcial, destino, esperado)
            return
        }

        pedir(contenido.url, desde).use { respuesta ->
            val retoma = respuesta.code == 206
            if (!respuesta.isSuccessful) {
                if (respuesta.code == 416) parcial.delete() // el tramo pedido no existe
                throw ErrorApi("No se pudo descargar ${contenido.id}: ${respuesta.code}")
            }
            if (desde > 0) {
                Log.i(TAG, if (retoma) "Retomando ${contenido.id} desde $desde bytes"
                           else "El servidor no acepto retomar ${contenido.id}: empieza de cero")
            }
            val cuerpo = respuesta.body ?: throw ErrorApi("Respuesta vacia")
            // Con 206 se agrega al final; con 200 el servidor mando el archivo
            // entero y se sobrescribe.
            FileOutputStream(parcial, retoma).use { salida ->
                cuerpo.byteStream().copyTo(salida)
            }
        }

        confirmar(parcial, destino, esperado)
    }

    /** Renombra el parcial al nombre definitivo solo si esta completo. */
    private fun confirmar(parcial: File, destino: File, esperado: Long?) {
        val bajado = parcial.length()
        if (esperado != null && bajado != esperado) {
            // Corto se conserva para retomar en el proximo intento; largo no
            // tiene arreglo y se descarta.
            if (bajado > esperado) parcial.delete()
            throw ErrorApi("${destino.name} incompleto: $bajado de $esperado bytes")
        }
        if (destino.exists()) destino.delete()
        if (!parcial.renameTo(destino)) {
            throw ErrorApi("No se pudo guardar ${destino.name}")
        }
    }

    private fun pedir(ruta: String, desde: Long = 0) = http.newCall(
        Request.Builder()
            .url("${identidad.servidor}$ruta")
            .header(HEADER_TOKEN, identidad.token)
            .apply { if (desde > 0) header("Range", "bytes=$desde-") }
            .build()
    ).execute()

    private companion object {
        const val HEADER_TOKEN = "X-Pantalla-Token"
        const val TAG = "ApiPlayer"
    }
}

/** JSONObject devuelve 0 para un campo null; aca hace falta distinguirlos. */
private fun JSONObject.optLongOrNull(clave: String): Long? =
    if (isNull(clave)) null else optLong(clave)

private fun JSONObject.optIntOrNull(clave: String): Int? =
    if (isNull(clave)) null else optInt(clave)
