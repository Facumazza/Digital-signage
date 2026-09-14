package com.grenlus.player

import android.content.Intent
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

    private fun volverAlPlayer() {
        startActivity(Intent(this, PlayerActivity::class.java))
        finish()
    }
}
