package com.grenlus.signage.dtos;

/**
 * Respuesta del alta de una pantalla. Es la unica vez que el token viaja en
 * claro: despues solo queda su hash en la base.
 *
 * Es el valor que hay que cargar en el dispositivo Android al instalarlo. Si
 * se pierde, no se recupera: se pide uno nuevo en POST /api/pantallas/{id}/token.
 */
public record PantallaCreadaDto(
        PantallaResponseDto pantalla,
        String tokenAcceso,
        String aviso
) {
    public PantallaCreadaDto(PantallaResponseDto pantalla, String tokenAcceso) {
        this(pantalla, tokenAcceso,
                "Guarda este token ahora: no se vuelve a mostrar.");
    }
}
