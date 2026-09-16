package com.grenlus.signage.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoPlayerDto {

    private Long id;
    private String tipo;
    private String url;
    private Integer duracion;

    /**
     * Tamanio exacto del archivo. El player lo usa para no dar por bueno un
     * archivo cortado, para retomar una descarga interrumpida y para saber si
     * le alcanza el espacio antes de empezar.
     */
    private Long tamanoBytes;
}