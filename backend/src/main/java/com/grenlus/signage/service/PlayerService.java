package com.grenlus.signage.service;

import com.grenlus.signage.dtos.ContenidoPlayerDto;
import com.grenlus.signage.dtos.PlayerConfigResponse;
import com.grenlus.signage.entity.Contenido;
import com.grenlus.signage.entity.Pantalla;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.entity.PlaylistContenido;
import com.grenlus.signage.exception.NoAutorizadoException;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.PantallaRepository;
import com.grenlus.signage.repository.PlaylistContenidoRepository;
import com.grenlus.signage.security.TokensPantalla;
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
    public PlayerConfigResponse obtenerConfiguracion(String codigo, String token) {

        Pantalla pantalla = autenticar(codigo, token);

        Playlist playlist = pantalla.getPlaylist();

        // Una pantalla recien dada de alta todavia no tiene playlist. No es un
        // error: se le responde que no hay nada que reproducir, y el player
        // sigue consultando hasta que la oficina le asigne una.
        if (playlist == null || !Boolean.TRUE.equals(pantalla.getActivo())) {
            return new PlayerConfigResponse(
                    pantalla.getCodigo(), null, null, estaEncendida(pantalla), List.of());
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
                                "/api/player/" + pantalla.getCodigo() + "/contenidos/"
                                        + item.getContenido().getId() + "/archivo",
                                item.getDuracionVisualizacion()))
                        .toList();

        // Apagada se mandan igual los contenidos: el player los conserva y al
        // volver a prenderla retoma al instante, sin descargar nada.
        return new PlayerConfigResponse(
                pantalla.getCodigo(),
                playlist.getId(),
                playlist.getVersion(),
                estaEncendida(pantalla),
                contenidos);
    }

    /**
     * Entrega el archivo de un contenido al player.
     *
     * Existe aparte de /api/contenidos/{id}/archivo porque aquel exige el JWT
     * del panel, y el Android no tiene uno: se autentica con su token de
     * pantalla. Sin esto el player podia leer su configuracion pero no
     * descargar nada de lo que esa configuracion le indicaba.
     *
     * Ademas solo entrega contenidos que esten en la playlist de esa pantalla:
     * un dispositivo no tiene por que poder bajar los archivos de otro cliente.
     */
    @Transactional(readOnly = true)
    public Contenido contenidoParaDescargar(String codigo, String token, Long contenidoId) {
        Pantalla pantalla = autenticar(codigo, token);
        Playlist playlist = pantalla.getPlaylist();

        if (playlist == null) {
            throw new RecursoNoEncontradoException(
                    "La pantalla no tiene contenido asignado");
        }

        return playlistContenidoRepository.findByPlaylistIdOrderByOrdenAsc(playlist.getId())
                .stream()
                .map(PlaylistContenido::getContenido)
                .filter(c -> c.getId().equals(contenidoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El contenido " + contenidoId + " no esta en la playlist de esta pantalla"));
    }

    /** Null solo en filas previas a la columna; se trata como encendida. */
    private boolean estaEncendida(Pantalla pantalla) {
        return !Boolean.FALSE.equals(pantalla.getEncendida());
    }

    /**
     * Marca que la pantalla sigue viva. Es lo unico que alimenta
     * ultimaConexion, de donde el panel deriva ONLINE/OFFLINE.
     */
    @Transactional
    public void heartbeat(String codigo, String token) {
        autenticar(codigo, token).setUltimaConexion(LocalDateTime.now());
    }

    /**
     * Identifica al dispositivo. El codigo dice quien dice ser; el token lo
     * demuestra. Se responde lo mismo si la pantalla no existe o si el token
     * esta mal: distinguirlos permitiria averiguar que codigos existen.
     */
    private Pantalla autenticar(String codigo, String token) {
        Pantalla pantalla = pantallaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoAutorizadoException("Codigo o token invalido"));

        if (!TokensPantalla.coincide(token, pantalla.getTokenHash())) {
            throw new NoAutorizadoException("Codigo o token invalido");
        }
        return pantalla;
    }
}
