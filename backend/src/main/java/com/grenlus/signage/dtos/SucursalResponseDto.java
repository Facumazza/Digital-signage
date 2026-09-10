package com.grenlus.signage.dtos;

/**
 * Lleva clienteNombre ademas del id porque el panel muestra nombres, no
 * numeros. Antes se devolvia la entidad con el Cliente entero anidado, lo que
 * hacia aparecer dos campos "id" distintos en la misma respuesta.
 */
public record SucursalResponseDto(
        Long id,
        String nombre,
        String direccion,
        String ciudad,
        String provincia,
        Boolean activo,
        Long clienteId,
        String clienteNombre
) {}
