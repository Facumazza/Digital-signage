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

        val faltantes = config.contenidos.filterNot { estaCompleto(it) }
        asegurarEspacio(config.contenidos, faltantes)

        faltantes.forEach { remoto ->
            Log.i(TAG, "Descargando ${remoto.id}")
            api.descargar(remoto, File(carpeta, nombreDe(remoto)))
        }

        val locales = config.contenidos.map { remoto ->
            ContenidoLocal(remoto.id, remoto.tipo, File(carpeta, nombreDe(remoto)), remoto.duracionSegundos)
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

    /**
     * Ya esta en el disco y entero. Un archivo con otro tamanio que el del
     * servidor se vuelve a bajar: puede haber quedado cortado antes de que
     * existiera esta verificacion.
     */
    private fun estaCompleto(remoto: ContenidoRemoto): Boolean {
        val archivo = File(carpeta, nombreDe(remoto))
        if (!archivo.exists()) return false
        if (remoto.tamanoBytes == null || archivo.length() == remoto.tamanoBytes) return true

        Log.w(TAG, "${archivo.name} mide ${archivo.length()} y deberia medir ${remoto.tamanoBytes}: se baja de nuevo")
        archivo.delete()
        return false
    }

    /**
     * Verifica que entre lo que falta bajar antes de empezar.
     *
     * Primero libera lo que no sirve ni para la playlist que se esta mostrando
     * ni para la nueva: la que se muestra no se toca, porque si la descarga
     * falla se sigue reproduciendo. Si igual no alcanza, corta con un error
     * claro en vez de llenar el disco a medias en cada intento.
     */
    private fun asegurarEspacio(nuevos: List<ContenidoRemoto>, faltantes: List<ContenidoRemoto>) {
        if (faltantes.isEmpty()) return

        val enUso = contenidosEnDisco().map { it.archivo.name }.toSet()
        val necesarios = nuevos.map { nombreDe(it) }.toSet()
        val conservar = enUso + necesarios + necesarios.map { "$it.parcial" } + "playlist.txt"
        carpeta.listFiles()?.forEach { archivo ->
            if (archivo.name !in conservar) {
                Log.i(TAG, "Liberando espacio: ${archivo.name}")
                archivo.delete()
            }
        }

        // Sin tamanio (backend viejo) no hay como calcularlo: se intenta igual.
        if (faltantes.any { it.tamanoBytes == null }) return

        val requerido = faltantes.sumOf { remoto ->
            val parcial = File(carpeta, "${nombreDe(remoto)}.parcial")
            remoto.tamanoBytes!! - parcial.length().coerceAtMost(remoto.tamanoBytes)
        }
        val disponible = carpeta.usableSpace
        if (requerido + MARGEN_BYTES > disponible) {
            throw EspacioInsuficiente(requerido, disponible)
        }
    }

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

        /**
         * Lo que se deja libre siempre. Un Android con el disco lleno empieza a
         * fallar en todo, no solo en esta app.
         */
        const val MARGEN_BYTES = 200L * 1024 * 1024
    }
}

class EspacioInsuficiente(requerido: Long, disponible: Long) : java.io.IOException(
    "Espacio insuficiente: la playlist necesita ${requerido / MB} MB mas y hay ${disponible / MB} MB libres"
) {
    private companion object {
        const val MB = 1024 * 1024
    }
}
