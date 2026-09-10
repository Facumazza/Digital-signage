package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SucursalRequestDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre,

        @Size(max = 200)
        String direccion,

        @Size(max = 100)
        String ciudad,

        @Size(max = 100)
        String provincia
) {}
