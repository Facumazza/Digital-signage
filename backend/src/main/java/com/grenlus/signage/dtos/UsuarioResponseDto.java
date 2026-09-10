package com.grenlus.signage.dtos;

import com.grenlus.signage.enums.Rol;

/**
 * Nunca incluye la contrasenia ni su hash. Antes se devolvia la entidad
 * completa, con lo cual un GET /api/usuarios exponia las credenciales de
 * todos los usuarios del sistema.
 */
public record UsuarioResponseDto(
        Long id,
        String nombre,
        String email,
        Rol rol,
        Boolean activo,
        Long clienteId,
        String clienteNombre
) {}
