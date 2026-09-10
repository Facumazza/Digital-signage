package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.PlayerConfigResponse;
import com.grenlus.signage.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{codigo}/config")
    public ResponseEntity<PlayerConfigResponse> obtenerConfiguracion(
            @PathVariable String codigo,
            @RequestHeader(value = HEADER_TOKEN, required = false) String token) {

        return ResponseEntity.ok(playerService.obtenerConfiguracion(codigo, token));
    }

    @PostMapping("/{codigo}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String codigo,
            @RequestHeader(value = HEADER_TOKEN, required = false) String token) {

        playerService.heartbeat(codigo, token);
        return ResponseEntity.ok().build();
    }
}
