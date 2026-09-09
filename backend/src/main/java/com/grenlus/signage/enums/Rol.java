package com.grenlus.signage.enums;

/**
 * Rol del usuario que accede al panel web.
 * SUPER_ADMIN es global (Usuario.cliente queda null); ADMIN_CLIENTE
 * solo ve los datos del cliente al que pertenece.
 * La seguridad real (JWT) recien se implementa en la Etapa 10.
 */
public enum Rol {
    SUPER_ADMIN,
    ADMIN_CLIENTE
}
