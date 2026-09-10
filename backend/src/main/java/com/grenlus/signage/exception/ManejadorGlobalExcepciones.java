package com.grenlus.signage.exception;

import com.grenlus.signage.dtos.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con sentido.
 *
 * Sin esto cualquier error termina en un 500 con stacktrace, que no le sirve
 * ni al frontend ni al player: los dos necesitan distinguir "no existe" de
 * "mandaste algo mal" de "se rompio el servidor".
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDto> noEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorResponseDto> noAutorizado(NoAutorizadoException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDto> reglaViolada(ReglaNegocioException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /**
     * Credenciales incorrectas en el login. A proposito no se distingue entre
     * "ese email no existe" y "la contrasenia esta mal": decirlo permitiria
     * averiguar que emails estan registrados.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> credenciales(AuthenticationException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Email o contrasenia incorrectos", null);
    }

    /** Autenticado, pero sin permiso para este recurso. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> sinPermiso(AccessDeniedException ex) {
        return construir(HttpStatus.FORBIDDEN, "No tenes permiso para esta operacion", null);
    }

    /** Se dispara cuando falla una validacion @Valid del DTO de entrada. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Datos invalidos", errores);
    }

    private ResponseEntity<ErrorResponseDto> construir(
            HttpStatus estado, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado).body(new ErrorResponseDto(
                LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, campos));
    }
}
