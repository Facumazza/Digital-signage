package com.grenlus.signage.service;

import com.grenlus.signage.entity.Contenido;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.entity.PlaylistContenido;
import com.grenlus.signage.repository.ContenidoRepository;
import com.grenlus.signage.repository.PlaylistContenidoRepository;
import com.grenlus.signage.repository.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistContenidoService {

    private final PlaylistContenidoRepository playlistContenidoRepository;
    private final PlaylistRepository playlistRepository;
    private final ContenidoRepository contenidoRepository;

    public PlaylistContenidoService(
            PlaylistContenidoRepository playlistContenidoRepository,
            PlaylistRepository playlistRepository,
            ContenidoRepository contenidoRepository
    ) {
        this.playlistContenidoRepository = playlistContenidoRepository;
        this.playlistRepository = playlistRepository;
        this.contenidoRepository = contenidoRepository;
    }

    public List<PlaylistContenido> listarPorPlaylist(Long playlistId) {

        return playlistContenidoRepository
                .findByPlaylistIdOrderByOrdenAsc(playlistId);
    }

    public PlaylistContenido buscarPorId(Long id) {

        return playlistContenidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Contenido de playlist no encontrado"));
    }

    public PlaylistContenido agregarContenido(
            Long playlistId,
            Long contenidoId,
            Integer orden,
            Integer duracionVisualizacion
    ) {

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() ->
                        new RuntimeException("Playlist no encontrada"));

        Contenido contenido = contenidoRepository.findById(contenidoId)
                .orElseThrow(() ->
                        new RuntimeException("Contenido no encontrado"));

        PlaylistContenido playlistContenido =
                new PlaylistContenido();

        playlistContenido.setPlaylist(playlist);
        playlistContenido.setContenido(contenido);
        playlistContenido.setOrden(orden);
        playlistContenido.setDuracionVisualizacion(
                duracionVisualizacion
        );
        playlistContenido.setActivo(true);

        PlaylistContenido guardado =
                playlistContenidoRepository.save(playlistContenido);

        aumentarVersion(playlist);

        return guardado;
    }

    public PlaylistContenido actualizar(
            Long id,
            PlaylistContenido datos
    ) {

        PlaylistContenido playlistContenido =
                buscarPorId(id);

        playlistContenido.setOrden(datos.getOrden());
        playlistContenido.setDuracionVisualizacion(
                datos.getDuracionVisualizacion()
        );
        playlistContenido.setActivo(datos.getActivo());

        PlaylistContenido actualizado =
                playlistContenidoRepository.save(
                        playlistContenido
                );

        aumentarVersion(playlistContenido.getPlaylist());

        return actualizado;
    }

    public void eliminar(Long id) {

        PlaylistContenido playlistContenido =
                buscarPorId(id);

        Playlist playlist = playlistContenido.getPlaylist();

        playlistContenidoRepository.delete(playlistContenido);

        aumentarVersion(playlist);
    }

    private void aumentarVersion(Playlist playlist) {

        if (playlist.getVersion() == null) {
            playlist.setVersion(1L);
        } else {
            playlist.setVersion(playlist.getVersion() + 1);
        }

        playlistRepository.save(playlist);
    }
}