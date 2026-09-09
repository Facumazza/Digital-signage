package com.grenlus.signage.service;

import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.enums.Rol;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));
    }

    public Usuario crear(Usuario usuario, Long clienteId) {

        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }

        if (usuario.getRol() == Rol.ADMIN_CLIENTE) {

            if (clienteId == null) {
                throw new RuntimeException(
                        "Un ADMIN_CLIENTE debe pertenecer a un cliente"
                );
            }

            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() ->
                            new RuntimeException("Cliente no encontrado"));

            usuario.setCliente(cliente);
        }

        if (usuario.getRol() == Rol.SUPER_ADMIN) {
            usuario.setCliente(null);
        }

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizar(Long id, Usuario datos) {

        Usuario usuario = buscarPorId(id);

        usuario.setNombre(datos.getNombre());
        usuario.setEmail(datos.getEmail());
        usuario.setRol(datos.getRol());
        usuario.setActivo(datos.getActivo());

        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {

        Usuario usuario = buscarPorId(id);

        usuarioRepository.delete(usuario);
    }
}