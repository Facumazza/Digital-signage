package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.AsignarPlaylistDto;
import com.grenlus.signage.dtos.PantallaRequestDto;
import com.grenlus.signage.dtos.PantallaResponseDto;
import com.grenlus.signage.service.PantallaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pantallas")
public class PantallaController {

    private final PantallaService pantallaService;

    public PantallaController(PantallaService pantallaService) {
        this.pantallaService = pantallaService;
    }

    @PostMapping
    public ResponseEntity<PantallaResponseDto> crear(@Valid @RequestBody PantallaRequestDto request) {
        PantallaResponseDto creada = pantallaService.crear(request);
        return ResponseEntity.created(URI.create("/api/pantallas/" + creada.id())).body(creada);
    }

    @GetMapping
    public List<PantallaResponseDto> listarPorSucursal(@RequestParam Long sucursalId) {
        return pantallaService.listarPorSucursal(sucursalId);
    }

    @GetMapping("/{id}")
    public PantallaResponseDto buscarPorId(@PathVariable Long id) {
        return pantallaService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public PantallaResponseDto actualizar(@PathVariable Long id,
                                          @Valid @RequestBody PantallaRequestDto request) {
        return pantallaService.actualizar(id, request);
    }

    /** Define que deberia reproducir esta pantalla. El player converge solo. */
    @PutMapping("/{id}/playlist")
    public PantallaResponseDto asignarPlaylist(@PathVariable Long id,
                                               @RequestBody AsignarPlaylistDto request) {
        return pantallaService.asignarPlaylist(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        pantallaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
