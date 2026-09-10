package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una pantalla. El codigo lo trae el dispositivo (flujo 6.1 de la
 * guia: el player lo genera y el administrador lo ingresa en el panel), por
 * eso viaja en el request y no lo inventa el backend.
 */
public record PantallaRequestDto(

        @NotBlank(message = "El codigo del dispositivo es obligatorio")
        @Size(max = 20, message = "El codigo no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @NotNull(message = "La pantalla debe pertenecer a una sucursal")
        Long sucursalId
) {}
