package com.grenlus.signage.controller;

import com.grenlus.signage.entity.PlaylistContenido;
import com.grenlus.signage.service.PlaylistContenidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlist-contenidos")
public class PlaylistContenidoController {

    private final PlaylistContenidoService playlistContenidoService;

    public PlaylistContenidoController(
            PlaylistContenidoService playlistContenidoService
    ) {
        this.playlistContenidoService =
                playlistContenidoService;
    }

    @GetMapping("/playlist/{playlistId}")
    public List<PlaylistContenido> listarPorPlaylist(
            @PathVariable Long playlistId
    ) {
        return playlistContenidoService
                .listarPorPlaylist(playlistId);
    }

    @GetMapping("/{id}")
    public PlaylistContenido buscarPorId(
            @PathVariable Long id
    ) {
        return playlistContenidoService.buscarPorId(id);
    }

    @PostMapping
    public PlaylistContenido agregarContenido(
            @RequestParam Long playlistId,
            @RequestParam Long contenidoId,
            @RequestParam Integer orden,
            @RequestParam(required = false)
            Integer duracionVisualizacion
    ) {
        return playlistContenidoService.agregarContenido(
                playlistId,
                contenidoId,
                orden,
                duracionVisualizacion
        );
    }

    @PutMapping("/{id}")
    public PlaylistContenido actualizar(
            @PathVariable Long id,
            @RequestBody PlaylistContenido playlistContenido
    ) {
        return playlistContenidoService.actualizar(
                id,
                playlistContenido
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {

        playlistContenidoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
