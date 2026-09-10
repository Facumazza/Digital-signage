package com.grenlus.signage.service;

import com.grenlus.signage.dtos.PlaylistContenidoRequestDto;
import com.grenlus.signage.dtos.PlaylistContenidoResponseDto;
import com.grenlus.signage.dtos.PlaylistContenidoUpdateDto;
import com.grenlus.signage.entity.Contenido;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.entity.PlaylistContenido;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.ContenidoRepository;
import com.grenlus.signage.repository.PlaylistContenidoRepository;
import com.grenlus.signage.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Composicion de una playlist: que contenidos tiene, en que orden y por
 * cuanto tiempo.
 *
 * Toda operacion que cambia la composicion incrementa Playlist.version. Ese
 * numero es lo unico que mira el player para decidir si tiene que
 * resincronizar, asi que un cambio que no lo toque es un cambio que ninguna
 * pantalla va a ver nunca.
 */
@Service
public class PlaylistContenidoService {

    private final PlaylistContenidoRepository playlistContenidoRepository;
    private final PlaylistRepository playlistRepository;
    private final ContenidoRepository contenidoRepository;

    public PlaylistContenidoService(PlaylistContenidoRepository playlistContenidoRepository,
                                    PlaylistRepository playlistRepository,
                                    ContenidoRepository contenidoRepository) {
        this.playlistContenidoRepository = playlistContenidoRepository;
        this.playlistRepository = playlistRepository;
        this.contenidoRepository = contenidoRepository;
    }

    @Transactional(readOnly = true)
    public List<PlaylistContenidoResponseDto> listarPorPlaylist(Long playlistId) {
        return playlistContenidoRepository.findByPlaylistIdOrderByOrdenAsc(playlistId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlaylistContenidoResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public PlaylistContenidoResponseDto agregar(PlaylistContenidoRequestDto request) {
        Playlist playlist = playlistRepository.findById(request.playlistId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Playlist", request.playlistId()));

        Contenido contenido = contenidoRepository.findById(request.contenidoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Contenido", request.contenidoId()));

        PlaylistContenido item = new PlaylistContenido();
        item.setPlaylist(playlist);
        item.setContenido(contenido);
        item.setOrden(request.orden());
        item.setDuracionVisualizacion(request.duracionVisualizacion());

        PlaylistContenido guardado = playlistContenidoRepository.save(item);
        aumentarVersion(playlist);
        return toResponse(guardado);
    }

    @Transactional
    public PlaylistContenidoResponseDto actualizar(Long id, PlaylistContenidoUpdateDto request) {
        PlaylistContenido item = obtener(id);
        item.setOrden(request.orden());
        item.setDuracionVisualizacion(request.duracionVisualizacion());
        if (request.activo() != null) {
            item.setActivo(request.activo());
        }
        aumentarVersion(item.getPlaylist());
        return toResponse(item);
    }

    /**
     * Borrado fisico, a diferencia del resto del sistema. Un item de playlist
     * no es una entidad del negocio que interese conservar: es la relacion
     * entre una playlist y un contenido. El contenido en si sigue existiendo.
     */
    @Transactional
    public void eliminar(Long id) {
        PlaylistContenido item = obtener(id);
        Playlist playlist = item.getPlaylist();
        playlistContenidoRepository.delete(item);
        aumentarVersion(playlist);
    }

    private void aumentarVersion(Playlist playlist) {
        playlist.setVersion(playlist.getVersion() == null ? 1L : playlist.getVersion() + 1);
    }

    private PlaylistContenido obtener(Long id) {
        return playlistContenidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Item de playlist", id));
    }

    private PlaylistContenidoResponseDto toResponse(PlaylistContenido item) {
        Contenido contenido = item.getContenido();
        return new PlaylistContenidoResponseDto(
                item.getId(),
                item.getPlaylist().getId(),
                item.getOrden(),
                item.getDuracionVisualizacion(),
                item.getActivo(),
                contenido.getId(),
                contenido.getNombre(),
                contenido.getTipo(),
                "/api/contenidos/" + contenido.getId() + "/archivo");
    }
}
