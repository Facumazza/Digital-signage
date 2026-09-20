package com.grenlus.signage.service;

import com.grenlus.signage.dtos.ContenidoResponseDto;
import com.grenlus.signage.dtos.ContenidoUpdateDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Contenido;
import com.grenlus.signage.enums.TipoContenido;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.exception.ReglaNegocioException;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.ContenidoRepository;
import com.grenlus.signage.security.ControlAcceso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContenidoService {

    private final ContenidoRepository contenidoRepository;
    private final ClienteRepository clienteRepository;
    private final ControlAcceso controlAcceso;
    private final Path directorioStorage;
    private final long duracionMaximaVideo;
    private final long tamanoMaximoVideoMb;
    private final long tamanoMaximoImagenMb;

    private static final long UN_MB = 1024L * 1024L;

    public ContenidoService(ContenidoRepository contenidoRepository,
                            ClienteRepository clienteRepository,
                            ControlAcceso controlAcceso,
                            @Value("${signage.storage.ruta}") String rutaStorage,
                            @Value("${signage.contenido.duracion-maxima-video}") long duracionMaximaVideo,
                            @Value("${signage.contenido.tamano-maximo-video-mb}") long tamanoMaximoVideoMb,
                            @Value("${signage.contenido.tamano-maximo-imagen-mb}") long tamanoMaximoImagenMb) {
        this.contenidoRepository = contenidoRepository;
        this.clienteRepository = clienteRepository;
        this.controlAcceso = controlAcceso;
        this.duracionMaximaVideo = duracionMaximaVideo;
        this.tamanoMaximoVideoMb = tamanoMaximoVideoMb;
        this.tamanoMaximoImagenMb = tamanoMaximoImagenMb;
        this.directorioStorage = Path.of(rutaStorage).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorioStorage);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de storage", e);
        }
    }

    /**
     * Guarda el archivo en disco y persiste sus metadatos.
     *
     * El nombre en disco lo genera el servidor (UUID + extension) y nunca se
     * usa el que mando el cliente: un nombre como "../../application.properties"
     * escribiria fuera del directorio de storage.
     */
    @Transactional
    public ContenidoResponseDto subir(MultipartFile archivo, String nombre,
                                      Long clienteId, Long duracionSegundos) {
        controlAcceso.verificar(clienteId);

        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo es obligatorio");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));

        TipoContenido tipo = deducirTipo(archivo.getContentType());
        if (tipo == TipoContenido.VIDEO) {
            validarDuracion(duracionSegundos);
        }
        validarTamano(tipo, archivo.getSize());

        String nombreOriginal = archivo.getOriginalFilename();
        String nombreEnDisco = UUID.randomUUID() + extensionDe(nombreOriginal);
        Path destino = directorioStorage.resolve(nombreEnDisco);

        try (var entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }

        Contenido contenido = Contenido.builder()
                .nombre(nombre)
                .tipo(tipo)
                .rutaArchivo(destino.toString())
                .nombreArchivo(nombreOriginal == null ? nombreEnDisco : nombreOriginal)
                .tamanoBytes(archivo.getSize())
                .duracionSegundos(tipo == TipoContenido.VIDEO ? duracionSegundos : null)
                .cliente(cliente)
                .build();

        return toResponse(contenidoRepository.save(contenido));
    }

    /**
     * Un video largo pesa mucho: tarda en llegar a cada pantalla, ocupa el
     * disco del TV Box y deja mas tiempo la cartelera mostrando lo mismo.
     *
     * La duracion la mide el panel al elegir el archivo y la manda al subir. El
     * backend no puede medirla solo: leer la de un MP4 necesita una libreria de
     * video. Por eso es obligatoria para los videos, aunque venga del cliente.
     */
    private void validarDuracion(Long duracionSegundos) {
        if (duracionSegundos == null) {
            throw new ReglaNegocioException(
                    "Falta la duracion del video. Subilo desde el panel, que la mide sola.");
        }
        if (duracionSegundos < 1) {
            throw new ReglaNegocioException("La duracion del video no es valida");
        }
        if (duracionSegundos > duracionMaximaVideo) {
            throw new ReglaNegocioException("El video dura " + duracionSegundos
                    + " segundos y el maximo es " + duracionMaximaVideo
                    + ". Recortalo antes de subirlo.");
        }
    }

    /**
     * Peso maximo por archivo. Esto el servidor si lo verifica solo, a
     * diferencia de la duracion: es el tope que no se puede esquivar llamando
     * la API a mano.
     */
    private void validarTamano(TipoContenido tipo, long bytes) {
        long maximoMb = tipo == TipoContenido.VIDEO ? tamanoMaximoVideoMb : tamanoMaximoImagenMb;
        if (bytes > maximoMb * UN_MB) {
            throw new ReglaNegocioException("El archivo pesa " + (bytes / UN_MB)
                    + " MB y el maximo para " + (tipo == TipoContenido.VIDEO ? "un video" : "una imagen")
                    + " es " + maximoMb + " MB");
        }
    }

    @Transactional(readOnly = true)
    public List<ContenidoResponseDto> listarPorCliente(Long clienteId) {
        controlAcceso.verificar(clienteId);
        return contenidoRepository.findByClienteIdAndActivoTrue(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContenidoResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public ContenidoResponseDto renombrar(Long id, ContenidoUpdateDto request) {
        Contenido contenido = obtener(id);
        contenido.setNombre(request.nombre());
        return toResponse(contenido);
    }

    /**
     * Baja logica. El archivo fisico queda en disco a proposito: alguna playlist
     * puede seguir referenciando este contenido, y un player sin conexion puede
     * estar reproduciendolo. Limpiar huerfanos es tarea de la Etapa 9.
     */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    /** La vuelta de la baja logica: sin esto, desactivar es irreversible. */
    @Transactional
    public void reactivar(Long id) {
        obtener(id).setActivo(true);
    }

    /** Devuelve el archivo para descargarlo. Es la url que consume el player. */
    @Transactional(readOnly = true)
    public Resource cargarArchivo(Long id) {
        Contenido contenido = obtener(id);
        try {
            Resource recurso = new UrlResource(Path.of(contenido.getRutaArchivo()).toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new RecursoNoEncontradoException(
                        "El archivo del contenido " + id + " no esta disponible");
            }
            return recurso;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    private Contenido obtener(Long id) {
        Contenido contenido = contenidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Contenido", id));

        // El player no tiene sesion de panel, asi que para el esta verificacion
        // no aplica: su acceso lo controla PlayerService con el token.
        controlAcceso.verificar(contenido.getCliente().getId());
        return contenido;
    }

    private TipoContenido deducirTipo(String contentType) {
        if (contentType == null) {
            throw new ReglaNegocioException("No se pudo determinar el tipo del archivo");
        }
        String tipo = contentType.toLowerCase(Locale.ROOT);
        if (tipo.startsWith("video/")) {
            return TipoContenido.VIDEO;
        }
        if (tipo.startsWith("image/")) {
            return TipoContenido.IMAGEN;
        }
        throw new ReglaNegocioException(
                "Solo se aceptan videos e imagenes. Se recibio: " + contentType);
    }

    private String extensionDe(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }
        int punto = nombreArchivo.lastIndexOf('.');
        return punto == -1 ? "" : nombreArchivo.substring(punto);
    }

    private ContenidoResponseDto toResponse(Contenido c) {
        return new ContenidoResponseDto(
                c.getId(),
                c.getNombre(),
                c.getTipo(),
                "/api/contenidos/" + c.getId() + "/archivo",
                c.getNombreArchivo(),
                c.getTamanoBytes(),
                c.getDuracionSegundos(),
                c.getFechaSubida(),
                c.getActivo(),
                c.getCliente().getId());
    }
}
