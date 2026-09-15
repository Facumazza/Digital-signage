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
import com.grenlus.signage.security.ControlAcceso;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final ControlAcceso controlAcceso;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          PasswordEncoder passwordEncoder,
                          ControlAcceso controlAcceso) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.controlAcceso = controlAcceso;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarTodos() {
        return usuarioRepository.findAll(Sort.by("nombre")).stream().map(this::toResponse).toList();
    }

    /** Los usuarios de un cliente, incluidos los dados de baja. */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarPorCliente(Long clienteId) {
        return usuarioRepository.findByClienteIdOrderByNombre(clienteId).stream()
                .map(this::toResponse).toList();
    }

    /** El equipo de Grenlus: los que administran todos los clientes. */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarSuperAdmins() {
        return usuarioRepository.findByRolOrderByNombre(Rol.SUPER_ADMIN).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public UsuarioResponseDto crear(UsuarioRequestDto request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + email);
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ReglaNegocioException("La contrasenia es obligatoria");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
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
        String email = normalizarEmail(request.email());

        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un usuario con el email " + email);
                });

        if (usuario.getRol() == Rol.SUPER_ADMIN && request.rol() != Rol.SUPER_ADMIN) {
            if (esUsuarioActual(id)) {
                throw new ReglaNegocioException("No podes quitarte el rol de SUPER_ADMIN a vos mismo");
            }
            exigirOtroSuperAdminActivo(usuario);
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
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
     * que. Corta el acceso en el acto: el filtro JWT rechaza a los inactivos
     * aunque su token no haya vencido.
     */
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = obtener(id);
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }
        if (esUsuarioActual(id)) {
            throw new ReglaNegocioException("No podes darte de baja a vos mismo");
        }
        if (usuario.getRol() == Rol.SUPER_ADMIN) {
            exigirOtroSuperAdminActivo(usuario);
        }
        usuario.setActivo(false);
    }

    /** La vuelta de la baja logica: sin esto, desactivar es irreversible. */
    @Transactional
    public void reactivar(Long id) {
        obtener(id).setActivo(true);
    }

    /**
     * Sin ningun SUPER_ADMIN activo nadie puede crear usuarios ni clientes, y
     * AdminInicial no lo arregla porque solo corre con la tabla vacia.
     */
    private void exigirOtroSuperAdminActivo(Usuario usuario) {
        long activos = usuarioRepository.countByRolAndActivoTrue(Rol.SUPER_ADMIN);
        long sinEste = Boolean.TRUE.equals(usuario.getActivo()) ? activos - 1 : activos;
        if (sinEste < 1) {
            throw new ReglaNegocioException(
                    "Es el ultimo SUPER_ADMIN activo: el sistema quedaria sin nadie que lo administre");
        }
    }

    private boolean esUsuarioActual(Long id) {
        return Objects.equals(controlAcceso.usuarioIdActual(), id);
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
