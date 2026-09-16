package com.grenlus.player.datos

import android.content.Context

/**
 * Quien es esta pantalla y que version de playlist tiene bajada.
 *
 * Vive en SharedPreferences porque tiene que sobrevivir a que se cierre la app
 * y a que se corte la luz: si el dispositivo olvidara su codigo habria que ir
 * hasta el local a reconfigurarlo.
 */
class Identidad(contexto: Context) : Credenciales {

    private val prefs =
        contexto.getSharedPreferences("grenlus.identidad", Context.MODE_PRIVATE)

    override var servidor: String
        get() = prefs.getString(SERVIDOR, "") ?: ""
        set(valor) = prefs.edit().putString(SERVIDOR, valor.trimEnd('/')).apply()

    override var codigo: String
        get() = prefs.getString(CODIGO, "") ?: ""
        set(valor) = prefs.edit().putString(CODIGO, valor.trim()).apply()

    override var token: String
        get() = prefs.getString(TOKEN, "") ?: ""
        set(valor) = prefs.edit().putString(TOKEN, valor.trim()).apply()

    /**
     * Version de la playlist que este dispositivo tiene efectivamente
     * descargada y lista. Se guarda recien cuando termino de bajar todo: si se
     * guardara antes, un corte a mitad de la descarga dejaria al player creyendo
     * que esta al dia con archivos que le faltan.
     */
    var versionLocal: Long
        get() = prefs.getLong(VERSION, -1)
        set(valor) = prefs.edit().putLong(VERSION, valor).apply()

    /**
     * Ultimo estado de encendido recibido. Se persiste para que una pantalla
     * apagada que se reinicia (corte de luz a la noche) no muestre contenido
     * hasta la primera consulta, que sin red podria no llegar nunca.
     */
    var encendida: Boolean
        get() = prefs.getBoolean(ENCENDIDA, true)
        set(valor) = prefs.edit().putBoolean(ENCENDIDA, valor).apply()

    /**
     * Si abre el player sola al prender el dispositivo. Encendido por defecto,
     * que es lo que necesita una TV de un local; se puede apagar para probar la
     * app en un celular personal sin que se abra en cada reinicio.
     */
    var arrancarAlEncender: Boolean
        get() = prefs.getBoolean(ARRANQUE, true)
        set(valor) = prefs.edit().putBoolean(ARRANQUE, valor).apply()

    val configurada: Boolean
        get() = servidor.isNotBlank() && codigo.isNotBlank() && token.isNotBlank()

    fun olvidar() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val SERVIDOR = "servidor"
        const val CODIGO = "codigo"
        const val TOKEN = "token"
        const val VERSION = "versionLocal"
        const val ENCENDIDA = "encendida"
        const val ARRANQUE = "arrancarAlEncender"
    }
}
