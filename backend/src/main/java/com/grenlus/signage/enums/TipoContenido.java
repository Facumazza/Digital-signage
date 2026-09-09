package com.grenlus.signage.enums;

/**
 * Tipo de archivo multimedia que puede reproducir una pantalla.
 * El player trata cada tipo distinto: el VIDEO dura lo que dura el archivo,
 * la IMAGEN dura lo que indique PlaylistContenido.duracionVisualizacion.
 */
public enum TipoContenido {
    VIDEO,
    IMAGEN
}
