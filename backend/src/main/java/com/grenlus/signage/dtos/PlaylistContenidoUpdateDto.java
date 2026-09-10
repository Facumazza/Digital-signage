package com.grenlus.signage.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cambia la posicion o la duracion de un item. No permite cambiar de que
 * contenido se trata: para eso se saca el item y se agrega otro, asi la
 * version de la playlist refleja el cambio real.
 */
public record PlaylistContenidoUpdateDto(

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 1, message = "El orden arranca en 1")
        Integer orden,

        @Min(value = 1, message = "La duracion debe ser de al menos 1 segundo")
        Integer duracionVisualizacion,

        Boolean activo
) {}
