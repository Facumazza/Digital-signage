package com.grenlus.signage.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequestDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 20)
        String cuit,

        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String telefono
) {}
