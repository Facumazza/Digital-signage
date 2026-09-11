package com.grenlus.signage.security;

import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puente entre la entidad Usuario y lo que Spring Security espera.
 *
 * Devuelve un UsuarioAutenticado y no el User generico de Spring porque la
 * sesion tiene que llevar el cliente al que pertenece: es lo que despues
 * permite aislar los datos de cada empresa.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No hay ningun usuario con el email " + email));

        // Dentro de la transaccion para que el cliente lazy se pueda cargar.
        return new UsuarioAutenticado(usuario);
    }
}
