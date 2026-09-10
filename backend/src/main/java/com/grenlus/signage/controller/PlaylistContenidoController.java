package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.PlaylistContenidoRequestDto;
import com.grenlus.signage.dtos.PlaylistContenidoResponseDto;
import com.grenlus.signage.dtos.PlaylistContenidoUpdateDto;
import com.grenlus.signage.service.PlaylistContenidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/playlist-contenidos")
public class PlaylistContenidoController {

    private final PlaylistContenidoService playlistContenidoService;

    public PlaylistContenidoController(PlaylistContenidoService playlistContenidoService) {
        this.playlistContenidoService = playlistContenidoService;
    }

    @GetMapping("/playlist/{playlistId}")
    public List<PlaylistContenidoResponseDto> listarPorPlaylist(@PathVariable Long playlistId) {
        return playlistContenidoService.listarPorPlaylist(playlistId);
    }

    @GetMapping("/{id}")
    public PlaylistContenidoResponseDto buscarPorId(@PathVariable Long id) {
        return playlistContenidoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<PlaylistContenidoResponseDto> agregar(
            @Valid @RequestBody PlaylistContenidoRequestDto request) {

        PlaylistContenidoResponseDto creado = playlistContenidoService.agregar(request);
        return ResponseEntity
                .created(URI.create("/api/playlist-contenidos/" + creado.id()))
                .body(creado);
    }

    @PutMapping("/{id}")
    public PlaylistContenidoResponseDto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlaylistContenidoUpdateDto request) {
        return playlistContenidoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        playlistContenidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
