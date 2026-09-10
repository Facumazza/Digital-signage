package com.grenlus.signage.entity;

import com.grenlus.signage.enums.TipoContenido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Archivo multimedia que puede reproducir una pantalla.
 *
 * En la base solo viven los metadatos: el archivo fisico va al storage y aca
 * se guarda donde quedo (rutaArchivo) y como se llamaba cuando lo subieron
 * (nombreArchivo).
 */
@Entity
@Table(name = "contenido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoContenido tipo;

    @NotBlank
    // La ruta interna del disco no sale por la API en ningun caso. Los

    // controllers que devuelven la entidad cruda publicaban rutas absolutas

    // del disco del servidor, que le dan a un atacante el mapa del sistema.

    @com.fasterxml.jackson.annotation.JsonIgnore

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @NotBlank
    @Column(name = "nombre_archivo", nullable = false, length = 500)
    private String nombreArchivo;

    @NotNull
    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    /** Solo aplica a VIDEO. En una IMAGEN queda null: cuanto se muestra lo
     *  decide PlaylistContenido.duracionVisualizacion, no el archivo. */
    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Column(nullable = false)
    private Boolean activo;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @PrePersist
    void alCrear() {
        if (activo == null) {
            activo = true;
        }
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }
}
