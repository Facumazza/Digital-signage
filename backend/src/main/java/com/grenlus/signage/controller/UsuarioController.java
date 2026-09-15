package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.UsuarioRequestDto;
import com.grenlus.signage.dtos.UsuarioResponseDto;
import com.grenlus.signage.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Sin filtros devuelve todos. Con clienteId, los de ese cliente; con
     * superAdmins=true, el equipo de Grenlus. Siempre incluye los inactivos,
     * para poder reactivarlos.
     */
    @GetMapping
    public List<UsuarioResponseDto> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(defaultValue = "false") boolean superAdmins) {
        if (clienteId != null) {
            return usuarioService.listarPorCliente(clienteId);
        }
        return superAdmins ? usuarioService.listarSuperAdmins() : usuarioService.listarTodos();
    }

    @GetMapping("/{id}")
    public UsuarioResponseDto buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crear(@Valid @RequestBody UsuarioRequestDto request) {
        UsuarioResponseDto creado = usuarioService.crear(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDto actualizar(@PathVariable Long id,
                                         @Valid @RequestBody UsuarioRequestDto request) {
        return usuarioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Vuelve a activar un usuario dado de baja. */
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}
