package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.ContenidoResponseDto;
import com.grenlus.signage.dtos.ContenidoUpdateDto;
import com.grenlus.signage.service.ContenidoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/contenidos")
public class ContenidoController {

    private final ContenidoService contenidoService;

    public ContenidoController(ContenidoService contenidoService) {
        this.contenidoService = contenidoService;
    }

    /**
     * El alta es multipart, no JSON: el archivo viaja como parte binaria y la
     * metadata como parametros. Por eso este endpoint no recibe un RequestDto
     * como el resto.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContenidoResponseDto> subir(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam String nombre,
            @RequestParam Long clienteId,
            @RequestParam(required = false) Long duracionSegundos) {

        ContenidoResponseDto creado =
                contenidoService.subir(archivo, nombre, clienteId, duracionSegundos);
        return ResponseEntity.created(URI.create("/api/contenidos/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<ContenidoResponseDto> listarPorCliente(@RequestParam Long clienteId) {
        return contenidoService.listarPorCliente(clienteId);
    }

    @GetMapping("/{id}")
    public ContenidoResponseDto buscarPorId(@PathVariable Long id) {
        return contenidoService.buscarPorId(id);
    }

    /** La url que consume el player para descargar el archivo. */
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) throws IOException {
        Resource archivo = contenidoService.cargarArchivo(id);

        String contentType = Files.probeContentType(archivo.getFile().toPath());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + archivo.getFilename() + "\"")
                .body(archivo);
    }

    @PutMapping("/{id}")
    public ContenidoResponseDto renombrar(@PathVariable Long id,
                                          @Valid @RequestBody ContenidoUpdateDto request) {
        return contenidoService.renombrar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        contenidoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
