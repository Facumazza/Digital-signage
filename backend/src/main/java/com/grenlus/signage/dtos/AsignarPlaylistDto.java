package com.grenlus.signage.dtos;

/**
 * Asigna una playlist a una pantalla. playlistId en null desasigna: la
 * pantalla queda sin contenido para reproducir.
 *
 * Esta operacion NO incrementa Playlist.version. La version identifica el
 * contenido de la playlist, no quien la reproduce.
 */
public record AsignarPlaylistDto(Long playlistId) {}
