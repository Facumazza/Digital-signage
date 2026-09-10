package com.grenlus.signage.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Agrega un contenido a una playlist, en una posicion dada. */
public record PlaylistContenidoRequestDto(

        @NotNull(message = "La playlist es obligatoria")
        Long playlistId,

        @NotNull(message = "El contenido es obligatorio")
        Long contenidoId,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 1, message = "El orden arranca en 1")
        Integer orden,

        /** Solo aplica a imagenes: un video dura lo que dura el archivo. */
        @Min(value = 1, message = "La duracion debe ser de al menos 1 segundo")
        Integer duracionVisualizacion
) {}
