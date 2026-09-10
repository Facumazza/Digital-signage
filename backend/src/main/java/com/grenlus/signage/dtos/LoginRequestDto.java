package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contrasenia es obligatoria")
        String password
) {}
