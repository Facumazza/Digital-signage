package com.grenlus.signage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS en un solo lugar, en vez de un @CrossOrigin repetido en cada controller.
 * El origen sale de configuracion: en produccion el panel no va a estar en
 * localhost:5173 y no queremos recompilar para cambiarlo.
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final String origenesPermitidos;

    public ConfiguracionWeb(@Value("${signage.cors.origenes}") String origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
