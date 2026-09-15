package com.grenlus.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.grenlus.player.datos.Identidad

/**
 * Abre el player cuando el dispositivo termina de encender.
 *
 * Una TV de un local se prende y se apaga con la luz, o con un corte. Sin esto
 * queda en el menu de Android hasta que alguien vaya a abrir la app a mano, y
 * nadie en el local sabe que tiene que hacerlo.
 *
 * Tambien reacciona a MY_PACKAGE_REPLACED: al instalar una version nueva del
 * APK, Android cierra la app y no la vuelve a abrir.
 */
class ArranqueAlEncender : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action !in ACCIONES) return

        val identidad = Identidad(contexto)
        // Sin configurar no se abre sola: aparecer en la pantalla de
        // configuracion cada vez que se reinicia un aparato sin usar molesta y
        // no muestra nada al publico.
        if (!identidad.configurada || !identidad.arrancarAlEncender) return

        if (!puedeAbrirseSola(contexto)) {
            // Android 10 en adelante bloquea abrir una pantalla desde segundo
            // plano. Se registra para que se vea en logcat al diagnosticar.
            Log.w(TAG, "Sin permiso para abrirse sola: falta 'Mostrar sobre otras apps'")
            return
        }

        Log.i(TAG, "Abriendo el player por ${intent.action}")
        contexto.startActivity(
            Intent(contexto, PlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    companion object {
        private const val TAG = "ArranqueAlEncender"

        private val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Algunos TV Box chinos no mandan BOOT_COMPLETED sino estos.
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )

        /**
         * Desde Android 10 una app no puede abrir una pantalla desde segundo
         * plano, y un receptor de arranque lo es. La excepcion que sirve aca es
         * tener el permiso "Mostrar sobre otras apps", que el instalador tiene
         * que conceder a mano una vez.
         */
        fun puedeAbrirseSola(contexto: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                Settings.canDrawOverlays(contexto)

        /** Pantalla de ajustes donde se concede ese permiso para esta app. */
        @RequiresApi(Build.VERSION_CODES.M)
        fun intentPermiso(contexto: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${contexto.packageName}")
            )
    }
}
