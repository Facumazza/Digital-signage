package com.grenlus.signage.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Decide si el usuario de la sesion actual puede tocar los datos de un cliente.
 *
 * El punto central: el cliente sale del token, nunca de la peticion. Si se
 * confiara en el clienteId que manda el navegador, cualquiera cambiaria ese
 * numero y leeria los datos de otra empresa, que es exactamente lo que pasaba
 * antes de esto.
 *
 * SUPER_ADMIN queda exento: administra todos los clientes.
 */
@Component
public class ControlAcceso {

    /** Null si es SUPER_ADMIN o si no hay sesion (endpoints del player). */
    public Long clienteDelUsuario() {
        UsuarioAutenticado usuario = usuarioActual();
        return usuario == null || usuario.esSuperAdmin() ? null : usuario.getClienteId();
    }

    public boolean esSuperAdmin() {
        UsuarioAutenticado usuario = usuarioActual();
        return usuario != null && usuario.esSuperAdmin();
    }

    /**
     * Corta la operacion si el recurso es de otro cliente.
     *
     * Se responde 403 y no 404: el usuario esta autenticado y el recurso
     * existe, simplemente no es suyo.
     */
    public void verificar(Long clienteIdDelRecurso) {
        Long propio = clienteDelUsuario();
        if (propio == null) {
            return;
        }
        if (!propio.equals(clienteIdDelRecurso)) {
            throw new AccessDeniedException(
                    "Ese recurso pertenece a otro cliente");
        }
    }

    /** Id del usuario de la sesion, o null si no hay nadie autenticado. */
    public Long usuarioIdActual() {
        UsuarioAutenticado usuario = usuarioActual();
        return usuario == null ? null : usuario.getUsuarioId();
    }

    private UsuarioAutenticado usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return null;
        }
        return usuario;
    }
}
