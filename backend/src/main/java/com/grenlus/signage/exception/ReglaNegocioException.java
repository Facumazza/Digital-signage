package com.grenlus.signage.exception;

/**
 * La peticion es sintacticamente valida pero viola una regla del negocio,
 * por ejemplo un codigo de pantalla repetido. Se traduce a 409 Conflict.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
