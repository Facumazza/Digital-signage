package com.grenlus.signage.controller;

import com.grenlus.signage.entity.Usuario;
import com.grenlus.signage.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> listarTodos() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    public Usuario crear(
            @RequestBody Usuario usuario,
            @RequestParam(required = false) Long clienteId
    ) {
        return usuarioService.crear(usuario, clienteId);
    }

    @PutMapping("/{id}")
    public Usuario actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario
    ) {
        return usuarioService.actualizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        usuarioService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}