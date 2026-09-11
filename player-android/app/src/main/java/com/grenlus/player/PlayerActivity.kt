package com.grenlus.player

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.grenlus.player.databinding.ActivityPlayerBinding
import com.grenlus.player.datos.ContenidoLocal
import com.grenlus.player.datos.Identidad
import com.grenlus.player.datos.Sincronizador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La pantalla que ve el publico. No tiene botones ni menu: se abre y no se
 * cierra nunca.
 *
 * Reproduce siempre desde el disco del dispositivo, nunca por streaming. Por
 * eso sigue funcionando con Internet cortado, que es el requisito central del
 * producto.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var vista: ActivityPlayerBinding
    private lateinit var identidad: Identidad
    private lateinit var sincronizador: Sincronizador

    private var exo: ExoPlayer? = null
    private var lista: List<ContenidoLocal> = emptyList()
    private var indice = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        identidad = Identidad(this)
        if (!identidad.configurada) {
            // Sin codigo ni token no hay nada que reproducir: se pide la
            // configuracion inicial.
            startActivity(Intent(this, ConfiguracionActivity::class.java))
            finish()
            return
        }

        vista = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(vista.root)

        // Una cartelera que se apaga sola no sirve de nada.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        pantallaCompleta()

        sincronizador = Sincronizador(this, identidad)

        // Arranca con lo que ya estaba bajado: si no hay Internet al prender,
        // igual muestra algo en vez de quedarse en negro esperando.
        lista = sincronizador.contenidosEnDisco()
        if (lista.isNotEmpty()) {
            mostrarEstado(null)
            reproducirDesde(0)
        } else {
            mostrarEstado("Esperando contenido…")
        }

        lanzarHeartbeat()
        lanzarSincronizacion()
    }

    /** Cada 20 segundos, como indica el flujo 6.5 de la guia. */
    private fun lanzarHeartbeat() = lifecycleScope.launch {
        while (isActive) {
            try {
                withContext(Dispatchers.IO) { sincronizador.enviarHeartbeat() }
            } catch (e: Exception) {
                // Sin Internet el heartbeat falla y esta bien: el panel va a
                // mostrar la pantalla como OFFLINE, que es la verdad.
                Log.w(TAG, "Heartbeat fallido: ${e.message}")
            }
            delay(20_000)
        }
    }

    private fun lanzarSincronizacion() = lifecycleScope.launch {
        while (isActive) {
            try {
                val nueva = withContext(Dispatchers.IO) { sincronizador.sincronizar() }

                // null significa "no cambio nada": se sigue reproduciendo sin
                // interrumpir. Cortar la reproduccion en cada consulta seria
                // un parpadeo cada 30 segundos.
                if (nueva != null && nueva.isNotEmpty()) {
                    Log.i(TAG, "Playlist nueva con ${nueva.size} contenidos")
                    lista = nueva
                    mostrarEstado(null)
                    reproducirDesde(0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sincronizacion fallida: ${e.message}")
                if (lista.isEmpty()) mostrarEstado("Sin conexion con el servidor")
            }
            delay(30_000)
        }
    }

    private fun reproducirDesde(posicion: Int) {
        if (lista.isEmpty()) return
        indice = posicion % lista.size
        val actual = lista[indice]

        if (actual.esVideo) mostrarVideo(actual) else mostrarImagen(actual)
    }

    private fun mostrarVideo(contenido: ContenidoLocal) {
        vista.imagen.visibility = View.GONE
        vista.video.visibility = View.VISIBLE

        val reproductor = exo ?: ExoPlayer.Builder(this).build().also {
            exo = it
            vista.video.player = it
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(estado: Int) {
                    // Al terminar el video se pasa al siguiente: asi la lista
                    // gira sola sin que nadie la toque.
                    if (estado == Player.STATE_ENDED) siguiente()
                }
            })
        }

        reproductor.setMediaItem(MediaItem.fromUri(contenido.archivo.toURI().toString()))
        reproductor.prepare()
        reproductor.play()
    }

    private fun mostrarImagen(contenido: ContenidoLocal) {
        exo?.pause()
        vista.video.visibility = View.GONE
        vista.imagen.visibility = View.VISIBLE
        vista.imagen.setImageURI(android.net.Uri.fromFile(contenido.archivo))

        // Una imagen no termina sola: hay que contarle el tiempo. Si no vino
        // duracion se usan 10 segundos, para no dejarla fija para siempre.
        val segundos = contenido.duracionSegundos ?: 10
        lifecycleScope.launch {
            val eraIndice = indice
            delay(segundos * 1000L)
            // Si mientras tanto entro una playlist nueva, este temporizador
            // quedo viejo y no debe hacer avanzar nada.
            if (isActive && indice == eraIndice) siguiente()
        }
    }

    private fun siguiente() = reproducirDesde(indice + 1)

    private fun mostrarEstado(texto: String?) {
        vista.estado.visibility = if (texto == null) View.GONE else View.VISIBLE
        vista.estado.text = texto
    }

    private fun pantallaCompleta() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onDestroy() {
        exo?.release()
        exo = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PlayerActivity"
    }
}
