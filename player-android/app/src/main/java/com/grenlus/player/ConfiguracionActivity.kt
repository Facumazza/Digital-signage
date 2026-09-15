package com.grenlus.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.grenlus.player.databinding.ActivityConfiguracionBinding
import com.grenlus.player.datos.Identidad

/**
 * Configuracion del dispositivo: servidor, codigo y token.
 *
 * Se abre sola la primera vez, y despues manteniendo apretada la pantalla del
 * player. Antes no habia forma de volver aca: para corregir un servidor mal
 * tipeado habia que borrar los datos de la app y pedir un token nuevo.
 */
class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var vista: ActivityConfiguracionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vista = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(vista.root)

        val identidad = Identidad(this)
        val yaConfigurada = identidad.configurada

        vista.servidor.setText(identidad.servidor.ifBlank { "http://192.168.1.36:8080" })
        vista.codigo.setText(identidad.codigo)

        // El token nunca se muestra, ni siquiera precargado. Esta pantalla se
        // abre tocando la TV de un local, donde puede estar mirando cualquiera.
        // Si el campo queda vacio se conserva el que ya tenia.
        if (yaConfigurada) {
            vista.token.setHint(R.string.token_mantener)
            vista.cancelar.visibility = View.VISIBLE
        }

        vista.arrancar.isChecked = identidad.arrancarAlEncender
        vista.arrancar.setOnCheckedChangeListener { _, activo ->
            identidad.arrancarAlEncender = activo
            mostrarEstadoArranque()
        }
        vista.darPermiso.setOnClickListener {
            // El boton solo se muestra desde Android 10, pero lint no lo sabe.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@setOnClickListener
            try {
                startActivity(ArranqueAlEncender.intentPermiso(this))
            } catch (e: ActivityNotFoundException) {
                // Varios Android TV y TV Box no traen esa pantalla de ajustes.
                Toast.makeText(this, R.string.permiso_sin_pantalla, Toast.LENGTH_LONG).show()
            }
        }

        vista.cancelar.setOnClickListener { volverAlPlayer() }

        vista.guardar.setOnClickListener {
            val servidor = vista.servidor.text.toString().trim()
            val codigo = vista.codigo.text.toString().trim()
            val tokenNuevo = vista.token.text.toString().trim()
            val token = tokenNuevo.ifBlank { identidad.token }

            if (servidor.isBlank() || codigo.isBlank() || token.isBlank()) {
                Toast.makeText(this, R.string.faltan_datos, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Si cambio el codigo, este aparato pasa a ser otra pantalla: lo
            // bajado corresponde a la anterior y hay que olvidarlo. Si solo
            // cambio el servidor o el token, es la misma pantalla y conviene
            // conservar lo descargado para no quedar en negro mientras resincroniza.
            if (codigo != identidad.codigo) {
                identidad.versionLocal = -1
            }

            identidad.servidor = servidor
            identidad.codigo = codigo
            identidad.token = token

            volverAlPlayer()
        }
    }

    /** En onResume y no en onCreate: el permiso se concede en otra pantalla y se vuelve aca. */
    override fun onResume() {
        super.onResume()
        mostrarEstadoArranque()
    }

    private fun mostrarEstadoArranque() {
        val activo = vista.arrancar.isChecked
        val conPermiso = ArranqueAlEncender.puedeAbrirseSola(this)

        vista.estadoArranque.setText(
            when {
                !activo -> R.string.arranque_apagado
                conPermiso -> R.string.arranque_listo
                else -> R.string.arranque_falta_permiso
            }
        )
        vista.darPermiso.visibility = if (activo && !conPermiso) View.VISIBLE else View.GONE
    }

    private fun volverAlPlayer() {
        startActivity(Intent(this, PlayerActivity::class.java))
        finish()
    }
}
