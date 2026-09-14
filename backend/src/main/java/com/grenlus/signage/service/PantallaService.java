package com.grenlus.signage.service;

import com.grenlus.signage.dtos.AsignarPlaylistDto;
import com.grenlus.signage.dtos.EncendidoDto;
import com.grenlus.signage.dtos.PantallaRequestDto;
import com.grenlus.signage.dtos.PantallaCreadaDto;
import com.grenlus.signage.dtos.PantallaResponseDto;
import com.grenlus.signage.entity.Pantalla;
import com.grenlus.signage.entity.Playlist;
import com.grenlus.signage.entity.Sucursal;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.exception.ReglaNegocioException;
import com.grenlus.signage.repository.PantallaRepository;
import com.grenlus.signage.repository.PlaylistRepository;
import com.grenlus.signage.security.TokensPantalla;
import com.grenlus.signage.repository.SucursalRepository;
import com.grenlus.signage.security.ControlAcceso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PantallaService {

    /**
     * El player manda heartbeat cada 20s (flujo 6.5). Un minuto da margen para
     * perder dos latidos por una red lenta antes de declarar la pantalla caida.
     */
    private static final Duration TOLERANCIA_ONLINE = Duration.ofMinutes(1);

    private final PantallaRepository pantallaRepository;
    private final SucursalRepository sucursalRepository;
    private final PlaylistRepository playlistRepository;
    private final ControlAcceso controlAcceso;

    public PantallaService(PantallaRepository pantallaRepository,
                           SucursalRepository sucursalRepository,
                           PlaylistRepository playlistRepository,
                           ControlAcceso controlAcceso) {
        this.pantallaRepository = pantallaRepository;
        this.sucursalRepository = sucursalRepository;
        this.playlistRepository = playlistRepository;
        this.controlAcceso = controlAcceso;
    }

    /**
     * Da de alta la pantalla y le genera su token. El token vuelve en claro
     * solo en esta respuesta: en la base queda su hash.
     */
    @Transactional
    public PantallaCreadaDto crear(PantallaRequestDto request) {
        if (pantallaRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException(
                    "Ya existe una pantalla con el codigo " + request.codigo());
        }
        Sucursal sucursal = sucursalVerificada(request.sucursalId());

        String token = TokensPantalla.generar();
        Pantalla pantalla = Pantalla.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .sucursal(sucursal)
                .build();
        pantalla.setTokenHash(TokensPantalla.hashear(token));

        return new PantallaCreadaDto(toResponse(pantallaRepository.save(pantalla)), token);
    }

    @Transactional(readOnly = true)
    public List<PantallaResponseDto> listarPorSucursal(Long sucursalId) {
        sucursalVerificada(sucursalId);
        return pantallaRepository.findBySucursalId(sucursalId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PantallaResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public PantallaResponseDto actualizar(Long id, PantallaRequestDto request) {
        Pantalla pantalla = obtener(id);

        if (!pantalla.getCodigo().equals(request.codigo())
                && pantallaRepository.existsByCodigo(request.codigo())) {
            throw new ReglaNegocioException(
                    "Ya existe una pantalla con el codigo " + request.codigo());
        }
        Sucursal sucursal = sucursalVerificada(request.sucursalId());

        pantalla.setCodigo(request.codigo());
        pantalla.setNombre(request.nombre());
        pantalla.setSucursal(sucursal);
        return toResponse(pantalla);
    }

    /**
     * Define el estado deseado de la pantalla. No le avisa a nadie: el player
     * lo descubre solo la proxima vez que consulte su configuracion. Por eso
     * asignar una playlist a una TV apagada no pierde la orden.
     */
    @Transactional
    public PantallaResponseDto asignarPlaylist(Long id, AsignarPlaylistDto request) {
        Pantalla pantalla = obtener(id);

        if (request.playlistId() == null) {
            pantalla.setPlaylist(null);
            return toResponse(pantalla);
        }
        Playlist playlist = playlistRepository.findById(request.playlistId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Playlist", request.playlistId()));

        // Sin esto se le podria asignar a una pantalla la playlist de otra
        // empresa, y esa TV terminaria mostrando publicidad ajena.
        controlAcceso.verificar(playlist.getCliente().getId());

        pantalla.setPlaylist(playlist);
        return toResponse(pantalla);
    }

    /**
     * Token nuevo, por ejemplo si se reemplaza el Android o se sospecha que se
     * filtro. El anterior deja de servir en el acto.
     */
    @Transactional
    public String regenerarToken(Long id) {
        String token = TokensPantalla.generar();
        obtener(id).setTokenHash(TokensPantalla.hashear(token));
        return token;
    }

    /**
     * Aplica la misma playlist a todas las pantallas activas de una sucursal.
     *
     * Es una comodidad, no una relacion nueva: cada pantalla sigue teniendo su
     * playlist propia, asi que despues se puede cambiar una sola sin afectar al
     * resto y el panel sigue mostrando que reproduce cada una.
     *
     * Solo toca las activas: una pantalla dada de baja no deberia volver a
     * entrar en servicio por un cambio masivo.
     */
    @Transactional
    public List<PantallaResponseDto> asignarPlaylistASucursal(Long sucursalId,
                                                              AsignarPlaylistDto request) {
        sucursalVerificada(sucursalId);

        Playlist playlist = null;
        if (request.playlistId() != null) {
            playlist = playlistRepository.findById(request.playlistId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Playlist", request.playlistId()));
            controlAcceso.verificar(playlist.getCliente().getId());
        }

        List<Pantalla> pantallas = pantallaRepository.findBySucursalId(sucursalId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();

        for (Pantalla pantalla : pantallas) {
            pantalla.setPlaylist(playlist);
        }
        return pantallas.stream().map(this::toResponse).toList();
    }

    /**
     * Prende o apaga una pantalla.
     *
     * Como todo lo demas, define el estado deseado y nada mas: el player se
     * entera en su proxima consulta, asi que tarda hasta 30 segundos en verse.
     * No toca la playlist ni la version, por eso al prenderla retoma lo que
     * tenia sin descargar nada.
     */
    @Transactional
    public PantallaResponseDto cambiarEncendido(Long id, EncendidoDto request) {
        Pantalla pantalla = obtener(id);
        pantalla.setEncendida(request.encendida());
        return toResponse(pantalla);
    }

    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    /** La vuelta de la baja logica: sin esto, desactivar es irreversible. */
    @Transactional
    public void reactivar(Long id) {
        obtener(id).setActivo(true);
    }

    private Pantalla obtener(Long id) {
        Pantalla pantalla = pantallaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pantalla", id));

        controlAcceso.verificar(clienteDe(pantalla.getSucursal()));
        return pantalla;
    }

    private Sucursal sucursalVerificada(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", sucursalId));
        controlAcceso.verificar(clienteDe(sucursal));
        return sucursal;
    }

    /** Una pantalla pertenece al cliente de su sucursal. */
    private Long clienteDe(Sucursal sucursal) {
        return sucursal == null || sucursal.getCliente() == null
                ? null : sucursal.getCliente().getId();
    }

    /** ONLINE/OFFLINE se calcula, no se guarda. Ver el comentario en Pantalla. */
    private String estadoDe(LocalDateTime ultimaConexion) {
        if (ultimaConexion == null) {
            return "OFFLINE";
        }
        return Duration.between(ultimaConexion, LocalDateTime.now())
                .compareTo(TOLERANCIA_ONLINE) <= 0 ? "ONLINE" : "OFFLINE";
    }

    private PantallaResponseDto toResponse(Pantalla p) {
        Playlist playlist = p.getPlaylist();
        return new PantallaResponseDto(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                estadoDe(p.getUltimaConexion()),
                p.getUltimaConexion(),
                p.getActivo(),
                !Boolean.FALSE.equals(p.getEncendida()),
                p.getSucursal().getId(),
                p.getSucursal().getNombre(),
                playlist == null ? null : playlist.getId(),
                playlist == null ? null : playlist.getNombre());
    }
}
