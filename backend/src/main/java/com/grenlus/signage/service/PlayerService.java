package com.grenlus.signage.service;

import com.grenlus.signage.dtos.ContenidoPlayerDto;
import com.grenlus.signage.dtos.PlayerConfigResponse;
import com.grenlus.signage.entity.Pantalla;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.PantallaRepository;
import com.grenlus.signage.repository.PlaylistContenidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Los dos endpoints que consume el Android.
 *
 * Es el unico punto del sistema donde se cruzan los dos dominios: lee la
 * pantalla y su sucursal por un lado, y la playlist con sus contenidos por el
 * otro.
 */
@Service
public class PlayerService {

    private final PantallaRepository pantallaRepository;
    private final PlaylistContenidoRepository playlistContenidoRepository;

    public PlayerService(PantallaRepository pantallaRepository,
                         PlaylistContenidoRepository playlistContenidoRepository) {
        this.pantallaRepository = pantallaRepository;
        this.playlistContenidoRepository = playlistContenidoRepository;
    }

    @Transactional(readOnly = true)
    public PlayerConfigResponse obtenerConfiguracion(String codigo) {

        Pantalla pantalla = pantallaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ninguna pantalla con el codigo " + codigo));

        Playlist playlist = pantalla.getPlaylist();

        // Una pantalla recien dada de alta todavia no tiene playlist. No es un
        // error: se le responde que no hay nada que reproducir, y el player
        // sigue consultando hasta que la oficina le asigne una.
        if (playlist == null || !Boolean.TRUE.equals(pantalla.getActivo())) {
            return new PlayerConfigResponse(pantalla.getCodigo(), null, null, List.of());
        }

        // Ordenado por la base, no en memoria: el repositorio existe para esto.
        List<ContenidoPlayerDto> contenidos =
                playlistContenidoRepository.findByPlaylistIdOrderByOrdenAsc(playlist.getId())
                        .stream()
                        .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                        .filter(item -> Boolean.TRUE.equals(item.getContenido().getActivo()))
                        .map(item -> new ContenidoPlayerDto(
                                item.getContenido().getId(),
                                item.getContenido().getTipo().name(),
                                // La url de descarga, NO rutaArchivo: esa es la
                                // ruta interna del disco del servidor y el
                                // Android no puede pedirla por HTTP.
                                "/api/contenidos/" + item.getContenido().getId() + "/archivo",
                                item.getDuracionVisualizacion()))
                        .toList();

        return new PlayerConfigResponse(
                pantalla.getCodigo(),
                playlist.getId(),
                playlist.getVersion(),
                contenidos);
    }

    /**
     * Marca que la pantalla sigue viva. Es lo unico que alimenta
     * ultimaConexion, de donde el panel deriva ONLINE/OFFLINE.
     */
    @Transactional
    public void heartbeat(String codigo) {
        Pantalla pantalla = pantallaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ninguna pantalla con el codigo " + codigo));

        pantalla.setUltimaConexion(LocalDateTime.now());
    }
}
