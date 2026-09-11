package com.grenlus.signage.service;

import com.grenlus.signage.dtos.PlaylistRequestDto;
import com.grenlus.signage.dtos.PlaylistResponseDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.PlaylistRepository;
import com.grenlus.signage.security.ControlAcceso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final ClienteRepository clienteRepository;
    private final ControlAcceso controlAcceso;

    public PlaylistService(PlaylistRepository playlistRepository,
                           ClienteRepository clienteRepository,
                           ControlAcceso controlAcceso) {
        this.playlistRepository = playlistRepository;
        this.clienteRepository = clienteRepository;
        this.controlAcceso = controlAcceso;
    }

    @Transactional
    public PlaylistResponseDto crear(PlaylistRequestDto request) {
        controlAcceso.verificar(request.clienteId());

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.clienteId()));

        Playlist playlist = Playlist.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .cliente(cliente)
                .build();
        return toResponse(playlistRepository.save(playlist));
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponseDto> listarPorCliente(Long clienteId) {
        controlAcceso.verificar(clienteId);
        return playlistRepository.findByClienteIdAndActivoTrue(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    /**
     * Cambia nombre y descripcion. NO toca version a proposito: renombrar una
     * playlist no cambia lo que reproduce la pantalla, y subir la version haria
     * que todas las TVs resincronicen y vuelvan a bajar archivos que ya tienen.
     * La version sube solo cuando cambia la composicion (agregar, quitar o
     * reordenar contenidos), y eso vive en PlaylistContenidoService.
     */
    @Transactional
    public PlaylistResponseDto actualizar(Long id, PlaylistRequestDto request) {
        Playlist playlist = obtener(id);
        playlist.setNombre(request.nombre());
        playlist.setDescripcion(request.descripcion());
        return toResponse(playlist);
    }

    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    private Playlist obtener(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Playlist", id));

        // Toda operacion sobre una playlist pasa por aca, asi que alcanza con
        // verificar en un solo lugar.
        controlAcceso.verificar(playlist.getCliente().getId());
        return playlist;
    }

    private PlaylistResponseDto toResponse(Playlist p) {
        return new PlaylistResponseDto(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getActivo(),
                p.getFechaCreacion(), p.getVersion(), p.getCliente().getId());
    }
}
