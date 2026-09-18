package com.grenlus.player

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.grenlus.player.databinding.ActivityPlayerBinding
import com.grenlus.player.datos.ContenidoLocal
import com.grenlus.player.datos.Identidad
import com.grenlus.player.datos.Imagenes
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

    /**
     * Cambia cada vez que se pasa a otro contenido, se apaga o se detiene.
     * Una imagen que termina de cargar o un temporizador que vence con otro
     * turno quedaron viejos y no deben tocar la pantalla.
     */
    private var turno = 0

    /** Contenidos seguidos que no se pudieron mostrar. Vuelve a 0 con uno que anda. */
    private var fallidosSeguidos = 0

    /** La oficina la apago: se muestra negro aunque haya contenido listo. */
    private var apagada = false

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
        // Si quedo apagada, arranca en negro: un corte de luz a la noche no
        // deberia encender la carteleria hasta que la oficina lo decida.
        apagada = !identidad.encendida
        when {
            apagada -> mostrarEstado(null)
            lista.isNotEmpty() -> {
                mostrarEstado(null)
                reproducirDesde(0)
            }
            else -> mostrarEstado("Esperando contenido…")
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
                val config = withContext(Dispatchers.IO) { sincronizador.consultar() }

                // Antes de descargar: prender o apagar tiene que verse en el
                // acto. Al prender se muestra lo que ya esta en disco, y si
                // hay playlist nueva se pasa a ella cuando termine de bajar.
                aplicarEncendido(config.encendida)

                val nueva = withContext(Dispatchers.IO) { sincronizador.sincronizar(config) }

                // null significa "no cambio nada": se sigue reproduciendo sin
                // interrumpir. Cortar la reproduccion en cada consulta seria
                // un parpadeo cada 30 segundos.
                when {
                    nueva == null -> Unit

                    // El servidor dijo que esta pantalla no tiene nada que
                    // mostrar: se apaga. Es como la oficina detiene una TV.
                    nueva.isEmpty() -> {
                        Log.i(TAG, "Sin contenido asignado: se detiene")
                        detener()
                    }

                    else -> {
                        Log.i(TAG, "Playlist nueva con ${nueva.size} contenidos")
                        lista = nueva
                        fallidosSeguidos = 0
                        mostrarEstado(null)
                        reproducirDesde(0)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sincronizacion fallida: ${e.message}")
                if (lista.isEmpty()) mostrarEstado("Sin conexion con el servidor")
            }
            delay(30_000)
        }
    }

    private fun reproducirDesde(posicion: Int) {
        // Apagada no se muestra nada, aunque lo pida el temporizador de una
        // imagen que estaba en pantalla al momento de apagar.
        if (lista.isEmpty() || apagada) return
        indice = posicion % lista.size
        turno++
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
                    when (estado) {
                        Player.STATE_READY -> fallidosSeguidos = 0
                        // Al terminar el video se pasa al siguiente: asi la
                        // lista gira sola sin que nadie la toque.
                        Player.STATE_ENDED -> siguiente()
                        Player.STATE_BUFFERING, Player.STATE_IDLE -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Sin esto un video que no se puede reproducir no terminaba
                    // nunca y la TV quedaba trabada en el. Pasa en TV Box
                    // baratos con videos 4K o H.265 que el celular si reproduce.
                    Log.w(TAG, "No se pudo reproducir " +
                        "${lista.getOrNull(indice)?.archivo?.name}: ${error.errorCodeName}")
                    contenidoFallido()
                }
            })
        }

        reproductor.setMediaItem(MediaItem.fromUri(contenido.archivo.toURI().toString()))
        reproductor.prepare()
        reproductor.play()
    }

    private fun mostrarImagen(contenido: ContenidoLocal) {
        exo?.pause()
        val miTurno = turno
        val pantalla = resources.displayMetrics
        val ancho = maxOf(pantalla.widthPixels, pantalla.heightPixels)
        val alto = minOf(pantalla.widthPixels, pantalla.heightPixels)

        lifecycleScope.launch {
            // Decodificar una foto grande tarda: fuera del hilo de la pantalla.
            val imagen = withContext(Dispatchers.IO) {
                Imagenes.cargarReducida(contenido.archivo, ancho, alto)
            }
            // Mientras cargaba pudo entrar otra playlist o apagarse la TV.
            if (miTurno != turno || apagada) return@launch

            if (imagen == null) {
                Log.w(TAG, "No se pudo mostrar la imagen ${contenido.archivo.name}")
                contenidoFallido()
                return@launch
            }

            vista.video.visibility = View.GONE
            vista.imagen.visibility = View.VISIBLE
            vista.imagen.setImageBitmap(imagen)
            fallidosSeguidos = 0

            // Una imagen no termina sola: hay que contarle el tiempo. Si no
            // vino duracion se usan 10 segundos, para no dejarla fija.
            delay((contenido.duracionSegundos ?: 10) * 1000L)
            if (miTurno == turno) siguiente()
        }
    }

    private fun siguiente() = reproducirDesde(indice + 1)

    /**
     * Saltea el contenido que no se pudo mostrar. Si fallan todos seguidos,
     * espera antes de reintentar: si no, una playlist de un solo video roto
     * giraria en falso a toda velocidad.
     */
    private fun contenidoFallido() {
        if (lista.isEmpty() || apagada) return
        fallidosSeguidos++
        if (fallidosSeguidos < lista.size) {
            siguiente()
            return
        }

        Log.w(TAG, "No se pudo mostrar ningun contenido: se reintenta en 30 s")
        fallidosSeguidos = 0
        turno++
        exo?.stop()
        vista.video.visibility = View.GONE
        vista.imagen.visibility = View.GONE
        mostrarEstado(getString(R.string.no_se_pudo_reproducir))

        val miTurno = turno
        lifecycleScope.launch {
            delay(30_000)
            if (miTurno == turno) {
                mostrarEstado(null)
                siguiente()
            }
        }
    }

    /*
     * Abrir la configuracion manteniendo apretado.
     *
     * Cinco segundos y no el toque largo de Android (medio segundo): la TV esta
     * en un local, y alguien apoyando la mano en un totem no deberia abrirla
     * por accidente. Funciona con el dedo en un celular y con el boton OK del
     * control remoto en un TV Box, que no tiene pantalla tactil.
     */

    private val abrirConfiguracion = Runnable {
        android.widget.Toast.makeText(
            this, R.string.abriendo_configuracion, android.widget.Toast.LENGTH_SHORT
        ).show()
        startActivity(Intent(this, ConfiguracionActivity::class.java))
        finish()
    }

    override fun dispatchTouchEvent(evento: android.view.MotionEvent): Boolean {
        // Si la app arranco sin configurar, onCreate redirige antes de crear la
        // vista: sin esta guarda un toque en ese instante la haria crashear.
        if (!::vista.isInitialized) return super.dispatchTouchEvent(evento)
        when (evento.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN ->
                vista.root.postDelayed(abrirConfiguracion, MANTENER_PARA_CONFIGURAR_MS)
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL ->
                vista.root.removeCallbacks(abrirConfiguracion)
        }
        return super.dispatchTouchEvent(evento)
    }

    override fun onKeyDown(codigo: Int, evento: android.view.KeyEvent): Boolean {
        // repeatCount == 0 es la primera pulsacion: mientras se mantiene,
        // Android repite el evento y no hay que reprogramar el temporizador.
        if (esBotonOk(codigo) && evento.repeatCount == 0 && ::vista.isInitialized) {
            vista.root.postDelayed(abrirConfiguracion, MANTENER_PARA_CONFIGURAR_MS)
            return true
        }
        return super.onKeyDown(codigo, evento)
    }

    override fun onKeyUp(codigo: Int, evento: android.view.KeyEvent): Boolean {
        if (esBotonOk(codigo) && ::vista.isInitialized) {
            vista.root.removeCallbacks(abrirConfiguracion)
            return true
        }
        return super.onKeyUp(codigo, evento)
    }

    private fun esBotonOk(codigo: Int) =
        codigo == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
            codigo == android.view.KeyEvent.KEYCODE_ENTER

    /** Deja la pantalla en negro con un aviso, sin cerrar la app. */
    private fun detener() {
        lista = emptyList()
        indice = 0
        turno++
        exo?.stop()
        vista.video.visibility = View.GONE
        vista.imagen.visibility = View.GONE
        if (!apagada) mostrarEstado("Sin contenido asignado")
    }

    /**
     * Pasa entre encendida y apagada solo cuando el estado cambia, para no
     * reiniciar la reproduccion en cada consulta.
     *
     * Apagar no borra nada: la lista y los archivos quedan, y al prender se
     * retoma desde donde estaba sin descargar de nuevo.
     */
    private fun aplicarEncendido(encendida: Boolean) {
        if (!encendida && !apagada) {
            Log.i(TAG, "Apagada desde el panel")
            apagada = true
            turno++
            exo?.pause()
            vista.video.visibility = View.GONE
            vista.imagen.visibility = View.GONE
            mostrarEstado(null)
        } else if (encendida && apagada) {
            Log.i(TAG, "Encendida desde el panel")
            apagada = false
            if (lista.isEmpty()) {
                mostrarEstado("Esperando contenido…")
            } else {
                mostrarEstado(null)
                reproducirDesde(indice)
            }
        }
    }

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
        if (::vista.isInitialized) vista.root.removeCallbacks(abrirConfiguracion)
        exo?.release()
        exo = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PlayerActivity"
        const val MANTENER_PARA_CONFIGURAR_MS = 5_000L
    }
}
