package com.grenlus.signage.controller;

import com.grenlus.signage.entity.Sucursal;
import com.grenlus.signage.service.SucursalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @GetMapping
    public List<Sucursal> listarTodas() {
        return sucursalService.listarTodas();
    }

    @GetMapping("/{id}")
    public Sucursal buscarPorId(@PathVariable Long id) {
        return sucursalService.buscarPorId(id);
    }

    @PostMapping("/cliente/{clienteId}")
    public Sucursal crear(
            @PathVariable Long clienteId,
            @RequestBody Sucursal sucursal
    ) {
        return sucursalService.crear(sucursal, clienteId);
    }

    @PutMapping("/{id}")
    public Sucursal actualizar(
            @PathVariable Long id,
            @RequestBody Sucursal sucursal
    ) {
        return sucursalService.actualizar(id, sucursal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        sucursalService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}