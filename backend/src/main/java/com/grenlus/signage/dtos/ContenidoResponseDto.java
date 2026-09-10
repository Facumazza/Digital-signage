package com.grenlus.signage.dtos;

import com.grenlus.signage.enums.TipoContenido;

import java.time.LocalDateTime;

/**
 * Lo que la API devuelve de un contenido.
 *
 * Expone "url" y no "rutaArchivo": la ruta es la ubicacion interna en el disco
 * del servidor y publicarla le da a un atacante el mapa del filesystem. La url
 * es ademas lo que consume el player, segun el contrato de la seccion 9.
 */
public record ContenidoResponseDto(
        Long id,
        String nombre,
        TipoContenido tipo,
        String url,
        String nombreArchivo,
        Long tamanoBytes,
        Long duracionSegundos,
        LocalDateTime fechaSubida,
        Boolean activo,
        Long clienteId
) {}
