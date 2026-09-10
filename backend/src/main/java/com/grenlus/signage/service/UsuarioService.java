package com.grenlus.signage.service;

import com.grenlus.signage.dtos.UsuarioRequestDto;
import com.grenlus.signage.dtos.UsuarioResponseDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.enums.Rol;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.exception.ReglaNegocioException;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarTodos() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public UsuarioResponseDto crear(UsuarioRequestDto request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + request.email());
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ReglaNegocioException("La contrasenia es obligatoria");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setRol(request.rol());
        // BCrypt genera un salt propio por usuario: dos personas con la misma
        // clave terminan con hashes distintos.
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setCliente(clienteDe(request));

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto actualizar(Long id, UsuarioRequestDto request) {
        Usuario usuario = obtener(id);

        usuarioRepository.findByEmail(request.email())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException(
                            "Ya existe un usuario con el email " + request.email());
                });

        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setRol(request.rol());
        usuario.setCliente(clienteDe(request));

        // La contrasenia solo cambia si mandaron una nueva.
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toResponse(usuario);
    }

    /**
     * Baja logica. Borrar el usuario fisicamente pierde el rastro de quien hizo
     * que, y UsuarioDetailsService ya rechaza el login de los inactivos.
     */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    /** SUPER_ADMIN es global; ADMIN_CLIENTE tiene que pertenecer a un cliente. */
    private Cliente clienteDe(UsuarioRequestDto request) {
        if (request.rol() == Rol.SUPER_ADMIN) {
            return null;
        }
        if (request.clienteId() == null) {
            throw new ReglaNegocioException("Un ADMIN_CLIENTE debe pertenecer a un cliente");
        }
        return clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    private UsuarioResponseDto toResponse(Usuario u) {
        Cliente cliente = u.getCliente();
        return new UsuarioResponseDto(
                u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.getActivo(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNombre());
    }
}
