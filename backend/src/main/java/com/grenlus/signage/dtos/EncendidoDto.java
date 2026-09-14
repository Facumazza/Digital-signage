package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotNull;

/** Prende o apaga una pantalla. */
public record EncendidoDto(

        @NotNull(message = "Hay que indicar si la pantalla queda encendida o apagada")
        Boolean encendida
) {}
