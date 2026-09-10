package com.grenlus.signage.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solo permite renombrar. El archivo no se modifica: para cambiarlo se sube
 * uno nuevo, asi las playlists que ya lo referencian no cambian de contenido
 * a espaldas de nadie.
 */
public record ContenidoUpdateDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre
) {}
