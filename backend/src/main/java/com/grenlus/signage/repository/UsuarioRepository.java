package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    /** El login no distingue mayusculas: Ana@x.com y ana@x.com son la misma persona. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByClienteIdOrderByNombre(Long clienteId);

    List<Usuario> findByRolOrderByNombre(Rol rol);

    /** Para no dejar el sistema sin nadie que lo administre. */
    long countByRolAndActivoTrue(Rol rol);
}
