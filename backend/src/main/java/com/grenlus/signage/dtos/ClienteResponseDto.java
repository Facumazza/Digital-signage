package com.grenlus.signage.dtos;

import java.time.LocalDateTime;

public record ClienteResponseDto(
        Long id,
        String nombre,
        String cuit,
        String email,
        String telefono,
        Boolean activo,
        LocalDateTime fechaAlta
) {}
