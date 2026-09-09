package com.grenlus.signage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Empresa que contrata el servicio. Es la raiz del modelo: todas las
 * sucursales, contenidos, playlists y usuarios cuelgan de un Cliente.
 *
 * Vive en la base compartida (no la toca ninguno de los dos dominios en
 * exclusiva) porque ambos lados del reparto la referencian.
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20)
    private String cuit;

    @Email
    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @PrePersist
    void alCrear() {
        if (activo == null) {
            activo = true;
        }
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
    }
}
