package com.grenlus.signage.security;

import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.enums.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * El usuario autenticado, con el cliente al que pertenece.
 *
 * Spring solo necesita usuario, contrasenia y roles, pero el aislamiento entre
 * clientes necesita saber de quien es cada sesion. Sin este dato, la unica
 * forma de saber a que cliente corresponde una peticion seria confiar en el id
 * que mande el navegador, que es justamente lo que no se puede hacer.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long usuarioId;
    private final String email;
    private final String passwordHash;
    private final Rol rol;
    private final Long clienteId;
    private final boolean activo;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol();
        this.clienteId = usuario.getCliente() == null ? null : usuario.getCliente().getId();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Rol getRol() {
        return rol;
    }

    /** Null para SUPER_ADMIN, que no pertenece a ningun cliente en particular. */
    public Long getClienteId() {
        return clienteId;
    }

    public boolean esSuperAdmin() {
        return rol == Rol.SUPER_ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring exige el prefijo ROLE_ para que hasRole() funcione.
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
