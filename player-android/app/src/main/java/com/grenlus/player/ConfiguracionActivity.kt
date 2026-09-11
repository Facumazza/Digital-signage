package com.grenlus.player

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.grenlus.player.databinding.ActivityConfiguracionBinding
import com.grenlus.player.datos.Identidad

/**
 * Configuracion inicial del dispositivo, la unica pantalla con la que alguien
 * interactua.
 *
 * Se usa una sola vez, al instalar: el instalador carga la direccion del
 * servidor y el codigo y token que le dio el panel al dar de alta la pantalla.
 * Despues no se vuelve a ver nunca.
 */
class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var vista: ActivityConfiguracionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vista = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(vista.root)

        val identidad = Identidad(this)
        vista.servidor.setText(identidad.servidor.ifBlank { "http://192.168.1.92:8080" })
        vista.codigo.setText(identidad.codigo)
        vista.token.setText(identidad.token)

        vista.guardar.setOnClickListener {
            val servidor = vista.servidor.text.toString().trim()
            val codigo = vista.codigo.text.toString().trim()
            val token = vista.token.text.toString().trim()

            if (servidor.isBlank() || codigo.isBlank() || token.isBlank()) {
                Toast.makeText(this, R.string.faltan_datos, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            identidad.servidor = servidor
            identidad.codigo = codigo
            identidad.token = token
            // Se olvida lo bajado: el dispositivo puede estar cambiando de
            // pantalla o de servidor, y la playlist vieja ya no aplica.
            identidad.versionLocal = -1

            startActivity(Intent(this, PlayerActivity::class.java))
            finish()
        }
    }
}
