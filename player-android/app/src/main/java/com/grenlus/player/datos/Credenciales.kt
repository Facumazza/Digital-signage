package com.grenlus.player.datos

/**
 * Lo unico que necesita ApiPlayer para hablar con el backend.
 *
 * Existe para que ApiPlayer no dependa de Identidad, que necesita un Context
 * de Android: asi la descarga se puede probar con tests comunes en la PC, sin
 * dispositivo ni emulador.
 */
interface Credenciales {
    val servidor: String
    val codigo: String
    val token: String
}
