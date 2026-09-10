package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.SucursalRequestDto;
import com.grenlus.signage.dtos.SucursalResponseDto;
import com.grenlus.signage.service.SucursalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @GetMapping
    public List<SucursalResponseDto> listarTodas() {
        return sucursalService.listarTodas();
    }

    @GetMapping("/{id}")
    public SucursalResponseDto buscarPorId(@PathVariable Long id) {
        return sucursalService.buscarPorId(id);
    }

    @PostMapping("/cliente/{clienteId}")
    public ResponseEntity<SucursalResponseDto> crear(
            @PathVariable Long clienteId,
            @Valid @RequestBody SucursalRequestDto request) {

        SucursalResponseDto creada = sucursalService.crear(request, clienteId);
        return ResponseEntity.created(URI.create("/api/sucursales/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public SucursalResponseDto actualizar(@PathVariable Long id,
                                          @Valid @RequestBody SucursalRequestDto request) {
        return sucursalService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        sucursalService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
