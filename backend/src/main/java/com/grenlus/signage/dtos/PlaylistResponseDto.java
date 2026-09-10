package com.grenlus.signage.dtos;

import java.time.LocalDateTime;

public record PlaylistResponseDto(
        Long id,
        String nombre,
        String descripcion,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Long version,
        Long clienteId
) {}
