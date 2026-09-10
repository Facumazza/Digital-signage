package com.grenlus.signage.config;

import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.enums.Rol;
import com.grenlus.signage.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer SUPER_ADMIN si la base no tiene ningun usuario.
 *
 * Sin esto el sistema queda cerrado sobre si mismo: el endpoint para crear
 * usuarios exige ser SUPER_ADMIN, y no hay ninguno.
 *
 * Solo corre con la base vacia de usuarios. Si ya hay alguno, no toca nada.
 */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${signage.admin-inicial.email}") String email,
                        @Value("${signage.admin-inicial.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Rol.SUPER_ADMIN);
        usuarioRepository.save(admin);

        log.warn("Se creo el SUPER_ADMIN inicial '{}'. Cambiar la contrasenia "
                + "antes de exponer el backend a Internet.", email);
    }
}
