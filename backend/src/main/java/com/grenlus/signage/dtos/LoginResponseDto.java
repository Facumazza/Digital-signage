package com.grenlus.signage.dtos;

/**
 * expiraEnMs le sirve al frontend para saber cuando pedir un token nuevo
 * antes de que el usuario se choque con un 401 en el medio de algo.
 */
public record LoginResponseDto(
        String token,
        String email,
        String rol,
        long expiraEnMs
) {}
