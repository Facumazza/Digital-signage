package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.PlayerConfigResponse;
import com.grenlus.signage.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/{codigo}/config")
    public ResponseEntity<PlayerConfigResponse> obtenerConfiguracion(
            @PathVariable String codigo
    ) {

        return ResponseEntity.ok(
                playerService.obtenerConfiguracion(codigo)
        );
    }

    @PostMapping("/{codigo}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String codigo
    ) {

        playerService.heartbeat(codigo);

        return ResponseEntity.ok().build();
    }
}