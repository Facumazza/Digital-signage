package com.grenlus.signage.dtos;

import com.grenlus.signage.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * La contrasenia viaja en texto plano una sola vez, al crear o al cambiarla,
 * y el service la hashea antes de guardarla. Es opcional porque al actualizar
 * un usuario no siempre se cambia la clave.
 */
public record UsuarioRequestDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150)
        String email,

        @Size(min = 8, message = "La contrasenia debe tener al menos 8 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        /** Obligatorio para ADMIN_CLIENTE, se ignora para SUPER_ADMIN. */
        Long clienteId
) {}
