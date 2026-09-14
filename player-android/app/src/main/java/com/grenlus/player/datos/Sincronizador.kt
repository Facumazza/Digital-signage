package com.grenlus.player.datos

import android.content.Context
import android.util.Log
import java.io.File

/** Un contenido ya descargado y listo para reproducir. */
data class ContenidoLocal(
    val id: Long,
    val tipo: String,
    val archivo: File,
    val duracionSegundos: Int?,
) {
    val esVideo: Boolean get() = tipo == "VIDEO"
}

/**
 * Mantiene el contenido del dispositivo al dia con lo que dice el servidor.
 *
 * La regla central del producto: la oficina define el estado deseado y este
 * dispositivo converge hacia el cuando puede. Nadie le empuja nada; el
 * pregunta.
 */
class Sincronizador(contexto: Context, private val identidad: Identidad) {

    private val api = ApiPlayer(identidad)
    private val carpeta = File(contexto.filesDir, "contenidos").apply { mkdirs() }

    /** Lo que hay guardado en el disco, sin consultar al servidor. */
    fun contenidosEnDisco(): List<ContenidoLocal> {
        val indice = File(carpeta, "playlist.txt")
        if (!indice.exists()) return emptyList()

        return indice.readLines().mapNotNull { linea ->
            val partes = linea.split("|")
            if (partes.size < 3) return@mapNotNull null

            val archivo = File(carpeta, partes[0])
            if (!archivo.exists()) return@mapNotNull null

            ContenidoLocal(
                id = partes[0].substringBefore('.').toLongOrNull() ?: return@mapNotNull null,
                tipo = partes[1],
                archivo = archivo,
                duracionSegundos = partes[2].toIntOrNull(),
            )
        }
    }

    /**
     * Consulta al servidor y, si cambio la version, descarga lo que falte.
     *
     * Devuelve la lista nueva cuando cambio algo, una lista vacia cuando el
     * servidor dice que no hay nada que mostrar, y null cuando ya estaba al
     * dia. Null significa "segui reproduciendo lo que tenias"; la lista vacia
     * significa "deja de mostrar".
     */
    fun sincronizar(): List<ContenidoLocal>? {
        val config = api.obtenerConfiguracion()

        // Primero que nada: prender o apagar no cambia la version, asi que si
        // esto quedara despues del chequeo de version nunca se aplicaria.
        identidad.encendida = config.encendida

        if (config.vacia) {
            // El servidor dice explicitamente que esta pantalla no tiene nada
            // que mostrar: se corta la reproduccion.
            //
            // Esto es distinto de no poder consultar. Sin red se sigue
            // reproduciendo (la excepcion la maneja quien llama); aca hubo
            // respuesta y la respuesta fue "nada". Tratar los dos casos igual
            // dejaba a la oficina sin forma de apagar una pantalla: sacarle la
            // playlist no la detenia, y habia que ir hasta el local.
            if (identidad.versionLocal != SIN_PLAYLIST) {
                Log.i(TAG, "La pantalla se quedo sin playlist: se detiene")
                File(carpeta, "playlist.txt").delete()
                // Se vuelve a -1 para que, si mas adelante le reasignan una
                // playlist, se considere nueva aunque sea la misma version.
                identidad.versionLocal = SIN_PLAYLIST
            }
            return emptyList()
        }

        if (config.playlistVersion == identidad.versionLocal) {
            // Mismo contenido que la ultima vez: no se toca nada. Este es el
            // caso normal, y es el que evita volver a bajar archivos que ya
            // estan en el disco.
            return null
        }

        Log.i(TAG, "Version ${identidad.versionLocal} -> ${config.playlistVersion}")

        val locales = config.contenidos.map { remoto ->
            val archivo = File(carpeta, nombreDe(remoto))

            if (!archivo.exists()) {
                Log.i(TAG, "Descargando ${remoto.id}")
                api.descargar(remoto, archivo)
            }

            ContenidoLocal(remoto.id, remoto.tipo, archivo, remoto.duracionSegundos)
        }

        // Recien aca, con todo bajado, se da por buena la version nueva. Si
        // algo fallo antes, salto una excepcion y versionLocal quedo como
        // estaba: el player sigue con la playlist anterior, completa, en vez
        // de mostrar una a medias.
        guardarIndice(locales)
        identidad.versionLocal = config.playlistVersion!!
        limpiarSobrantes(locales)

        return locales
    }

    /** Estado de encendido segun la ultima respuesta del servidor. */
    val encendida: Boolean get() = identidad.encendida

    fun enviarHeartbeat() = api.enviarHeartbeat()

    /** El id manda: dos contenidos distintos nunca comparten nombre de archivo. */
    private fun nombreDe(remoto: ContenidoRemoto): String {
        val extension = remoto.url.substringAfterLast('.', "").takeIf { it.length in 1..4 }
        return if (extension != null) "${remoto.id}.$extension" else "${remoto.id}.bin"
    }

    private fun guardarIndice(contenidos: List<ContenidoLocal>) {
        File(carpeta, "playlist.txt").writeText(
            contenidos.joinToString("\n") { c ->
                "${c.archivo.name}|${c.tipo}|${c.duracionSegundos ?: ""}"
            }
        )
    }

    /**
     * Borra los archivos que ya no estan en la playlist. Sin esto el
     * almacenamiento de un TV Box se llena con meses de promociones viejas.
     */
    private fun limpiarSobrantes(vigentes: List<ContenidoLocal>) {
        val nombres = vigentes.map { it.archivo.name }.toSet() + "playlist.txt"
        carpeta.listFiles()?.forEach { archivo ->
            if (archivo.name !in nombres) {
                Log.i(TAG, "Borrando sobrante ${archivo.name}")
                archivo.delete()
            }
        }
    }

    private companion object {
        const val TAG = "Sincronizador"

        /** Ninguna playlist bajada: cualquier version futura cuenta como nueva. */
        const val SIN_PLAYLIST = -1L
    }
}
