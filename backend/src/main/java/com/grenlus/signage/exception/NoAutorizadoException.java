package com.grenlus.signage.exception;

/**
 * Credenciales ausentes o invalidas en un endpoint que las exige. Se traduce
 * a 401.
 */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
