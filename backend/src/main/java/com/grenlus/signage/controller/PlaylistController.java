package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.PlaylistRequestDto;
import com.grenlus.signage.dtos.PlaylistResponseDto;
import com.grenlus.signage.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping
    public ResponseEntity<PlaylistResponseDto> crear(@Valid @RequestBody PlaylistRequestDto request) {
        PlaylistResponseDto creada = playlistService.crear(request);
        return ResponseEntity.created(URI.create("/api/playlists/" + creada.id())).body(creada);
    }

    @GetMapping
    public List<PlaylistResponseDto> listarPorCliente(@RequestParam Long clienteId) {
        return playlistService.listarPorCliente(clienteId);
    }

    @GetMapping("/{id}")
    public PlaylistResponseDto buscarPorId(@PathVariable Long id) {
        return playlistService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public PlaylistResponseDto actualizar(@PathVariable Long id,
                                          @Valid @RequestBody PlaylistRequestDto request) {
        return playlistService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        playlistService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Vuelve a activar un recurso dado de baja. */
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        playlistService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}
