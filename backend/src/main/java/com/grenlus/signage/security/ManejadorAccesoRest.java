package com.grenlus.signage.security;

import tools.jackson.databind.ObjectMapper;
import com.grenlus.signage.dtos.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Respuestas de seguridad con la misma forma que el resto de los errores.
 *
 * Sin esto Spring devuelve 403 tambien cuando el problema es que no hay
 * sesion, y el frontend no puede distinguir "logueate" de "no tenes permiso":
 * con un 403 no sabe si mandar al login o mostrar un cartel.
 */
@Component
public class ManejadorAccesoRest implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ManejadorAccesoRest(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** No hay credenciales o no sirven: 401. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED,
                "Necesitas iniciar sesion para acceder a este recurso");
    }

    /** Autenticado, pero el rol no alcanza: 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN,
                "No tenes permiso para esta operacion");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponseDto(
                LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, null));
    }
}
