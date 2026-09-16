package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.PlayerConfigResponse;
import com.grenlus.signage.entity.Contenido;
import com.grenlus.signage.service.ContenidoService;
import com.grenlus.signage.service.PlayerService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Los dos endpoints que consume el Android.
 *
 * No usan JWT como el panel: un televisor no puede completar un formulario de
 * login. Cada pantalla se identifica con su codigo en la URL y su token en el
 * header X-Pantalla-Token, que se le entrega al darla de alta.
 */
@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private static final String HEADER_TOKEN = "X-Pantalla-Token";

    private final PlayerService playerService;
    private final ContenidoService contenidoService;

    public PlayerController(PlayerService playerService,
                            ContenidoService contenidoService) {
        this.playerService = playerService;
        this.contenidoService = contenidoService;
    }

    @GetMapping("/{codigo}/config")
    public ResponseEntity<PlayerConfigResponse> obtenerConfiguracion(
            @PathVariable String codigo,
            @RequestHeader(value = HEADER_TOKEN, required = false) String token) {

        return ResponseEntity.ok(playerService.obtenerConfiguracion(codigo, token));
    }

    /**
     * Descarga de un archivo por parte del player.
     *
     * No puede usar /api/contenidos/{id}/archivo: ese exige el JWT del panel y
     * el Android no tiene uno.
     *
     * Acepta el header Range: al devolver un Resource con 200, Spring responde
     * 206 con solo el tramo pedido. Es lo que permite al player retomar una
     * descarga cortada en vez de empezar de cero.
     */
    @GetMapping("/{codigo}/contenidos/{contenidoId}/archivo")
    public ResponseEntity<Resource> descargar(
            @PathVariable String codigo,
            @PathVariable Long contenidoId,
            @RequestHeader(value = HEADER_TOKEN, required = false) String token)
            throws IOException {

        Contenido contenido =
                playerService.contenidoParaDescargar(codigo, token, contenidoId);
        Resource archivo = contenidoService.cargarArchivo(contenido.getId());

        String tipoMime = Files.probeContentType(archivo.getFile().toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        tipoMime != null ? tipoMime : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(archivo);
    }

    @PostMapping("/{codigo}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String codigo,
            @RequestHeader(value = HEADER_TOKEN, required = false) String token) {

        playerService.heartbeat(codigo, token);
        return ResponseEntity.ok().build();
    }
}
