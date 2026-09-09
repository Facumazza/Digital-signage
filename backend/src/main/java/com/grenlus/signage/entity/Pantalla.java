package com.grenlus.signage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dispositivo logico asociado a una TV, monitor, LED o totem.
 *
 * No se llama TV a proposito: el producto tiene que soportar cualquier
 * pantalla, no solo televisores.
 *
 * No existe un campo "estado": ONLINE/OFFLINE se deriva de ultimaConexion
 * (ONLINE si esta dentro del ultimo minuto) y ese calculo vive en el service,
 * no aca. Guardar el estado obligaria a mantenerlo sincronizado y a decidir
 * quien lo apaga cuando una pantalla deja de responder.
 */
@Entity
@Table(name = "pantalla")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pantalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador fisico del dispositivo, ej. GRN-A8K91X. Es la clave con
     *  la que el player se presenta ante la API, asi que no puede repetirse. */
    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Null mientras la pantalla nunca se haya conectado. No confundir "nunca
     *  se conecto" con "se conecto hace mucho": son estados distintos. */
    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    @Column(nullable = false)
    private Boolean activo;

    // TODO: agregar cuando Sucursal exista en develop.
    // Segun la guia la relacion es obligatoria: N Pantallas -> 1 Sucursal.
    // @ManyToOne
    // @JoinColumn(name = "sucursal_id", nullable = false)
    // private Sucursal sucursal;

    /** Nullable a proposito: una pantalla se da de alta antes de que se le
     *  asigne contenido. Sin playlist simplemente no reproduce nada. */
    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @PrePersist
    void alCrear() {
        if (activo == null) {
            activo = true;
        }
    }
}
