package com.grenlus.signage.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responde si el backend esta vivo.
 *
 * Lo usa el hosting para saber si un deploy quedo bien: si la app no arranca,
 * conviene que siga corriendo la version anterior en vez de dejar las
 * pantallas sin servidor. Tambien sirve para probar desde el celular si el
 * backend se alcanza, sin necesidad de un token.
 *
 * No dice nada del sistema a proposito: version, base de datos o memoria son
 * datos utiles para quien quiera atacarlo.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    @GetMapping
    public Map<String, String> salud() {
        return Map.of("estado", "ok");
    }
}
