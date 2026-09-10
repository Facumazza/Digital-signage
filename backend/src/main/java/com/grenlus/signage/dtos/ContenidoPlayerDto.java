package com.grenlus.signage.dto;

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
}