package com.grenlus.signage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerConfigResponse {

    private String pantalla;
    private Long playlistId;
    private Long playlistVersion;
    private List<ContenidoPlayerDTO> contenidos;
}