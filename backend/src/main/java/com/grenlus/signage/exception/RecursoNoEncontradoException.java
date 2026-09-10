package com.grenlus.signage.exception;

/**
 * El recurso pedido no existe. El manejador global la traduce a 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " no encontrado");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
