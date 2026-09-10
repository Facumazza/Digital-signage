package com.grenlus.signage.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Forma unica de los errores de la API. "campos" solo aparece en el JSON
 * cuando hay errores de validacion por campo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        LocalDateTime momento,
        int estado,
        String error,
        String mensaje,
        Map<String, String> campos
) {}
