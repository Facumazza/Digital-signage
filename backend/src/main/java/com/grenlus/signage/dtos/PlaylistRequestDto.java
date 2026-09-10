package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaylistRequestDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 300)
        String descripcion,

        @NotNull(message = "La playlist debe pertenecer a un cliente")
        Long clienteId
) {}
