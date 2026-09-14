package com.grenlus.signage.dtos;

import java.time.LocalDateTime;

/**
 * Estado de una pantalla para el panel.
 *
 * "estado" no existe como columna: se deriva de ultimaConexion en el service.
 * Es el ejemplo de por que un DTO no es un espejo de la entidad, sino la forma
 * que necesita quien consume la API.
 *
 * Incluye los nombres de sucursal y playlist ademas de los ids porque el panel
 * muestra "Promos Septiembre", no "3".
 */
public record PantallaResponseDto(
        Long id,
        String codigo,
        String nombre,
        String estado,
        LocalDateTime ultimaConexion,
        Boolean activo,
        Boolean encendida,
        Long sucursalId,
        String sucursalNombre,
        Long playlistId,
        String playlistNombre
) {}
