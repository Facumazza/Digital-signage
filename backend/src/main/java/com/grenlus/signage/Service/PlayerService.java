package com.grenlus.signage.service;

import com.grenlus.signage.dto.ContenidoPlayerDTO;
import com.grenlus.signage.dto.PlayerConfigResponse;
import com.grenlus.signage.entity.Pantalla;
import com.grenlus.signage.entity.PlaylistContenido;
import com.grenlus.signage.repository.PantallaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlayerService {

    private final PantallaRepository pantallaRepository;

    public PlayerService(PantallaRepository pantallaRepository) {
        this.pantallaRepository = pantallaRepository;
    }

    public PlayerConfigResponse obtenerConfiguracion(String codigo) {

        Pantalla pantalla = pantallaRepository.findByCodigo(codigo)
                .orElseThrow(() ->
                        new RuntimeException("Pantalla no encontrada"));

        if (!pantalla.getActivo()) {
            throw new RuntimeException("La pantalla está inactiva");
        }

        if (pantalla.getPlaylist() == null) {
            throw new RuntimeException(
                    "La pantalla no tiene una playlist asignada");
        }

        List<ContenidoPlayerDTO> contenidos =
                pantalla.getPlaylist()
                        .getContenidos()
                        .stream()
                        .filter(PlaylistContenido::getActivo)
                        .sorted((a, b) ->
                                Integer.compare(
                                        a.getOrden(),
                                        b.getOrden()))
                        .map(item -> new ContenidoPlayerDTO(
                                item.getContenido().getId(),
                                item.getContenido().getTipo().name(),
                                item.getContenido().getRutaArchivo(),
                                item.getDuracionVisualizacion()
                        ))
                        .toList();

        return new PlayerConfigResponse(
                pantalla.getCodigo(),
                pantalla.getPlaylist().getId(),
                pantalla.getPlaylist().getVersion(),
                contenidos
        );
    }

    public void heartbeat(String codigo) {

        Pantalla pantalla = pantallaRepository.findByCodigo(codigo)
                .orElseThrow(() ->
                        new RuntimeException("Pantalla no encontrada"));

        pantalla.setUltimaConexion(LocalDateTime.now());

        pantallaRepository.save(pantalla);
    }
}