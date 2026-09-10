package com.grenlus.signage.dtos;

import com.grenlus.signage.enums.TipoContenido;

/**
 * Un item de playlist, aplanado. El panel necesita mostrar el nombre y la
 * miniatura del contenido junto al orden, y anidar la entidad Contenido
 * completa arrastraba tambien su cliente y su ruta en disco.
 */
public record PlaylistContenidoResponseDto(
        Long id,
        Long playlistId,
        Integer orden,
        Integer duracionVisualizacion,
        Boolean activo,
        Long contenidoId,
        String contenidoNombre,
        TipoContenido tipo,
        String url
) {}
