package com.grenlus.signage.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Genera y verifica los tokens con los que se identifica cada pantalla.
 *
 * Se guarda el hash, no el token: si alguien lee la base no puede hacerse
 * pasar por ninguna pantalla. Como contrapartida, un token perdido no se
 * recupera, se regenera.
 *
 * Se usa SHA-256 y no BCrypt, al reves que con las contrasenias de usuario.
 * BCrypt es lento a proposito para que no se puedan probar millones de claves
 * humanas por segundo, pero aca el heartbeat pega cada 20 segundos por
 * pantalla y el token son 256 bits aleatorios: no hay diccionario que probar,
 * asi que el costo no compra nada.
 */
public final class TokensPantalla {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokensPantalla() {
    }

    /** Token nuevo, en hexadecimal. Es el unico momento en que existe en claro. */
    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hashear(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 deberia estar siempre disponible", e);
        }
    }

    /**
     * Comparacion en tiempo constante. Un equals normal corta en el primer
     * caracter distinto, y ese tiempo de respuesta permite ir adivinando el
     * token caracter por caracter.
     */
    public static boolean coincide(String token, String hashGuardado) {
        if (token == null || hashGuardado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hashear(token).getBytes(StandardCharsets.UTF_8),
                hashGuardado.getBytes(StandardCharsets.UTF_8));
    }
}
