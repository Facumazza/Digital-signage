package com.grenlus.signage.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "playlist_contenidos")
public class PlaylistContenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne
    @JoinColumn(name = "contenido_id", nullable = false)
    private Contenido contenido;

    @Column(nullable = false)
    private Integer orden;

    private Integer duracionVisualizacion;

    @Column(nullable = false)
    private Boolean activo = true;

    public PlaylistContenido() {
    }

    public Long getId() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public Contenido getContenido() {
        return contenido;
    }

    public void setContenido(Contenido contenido) {
        this.contenido = contenido;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getDuracionVisualizacion() {
        return duracionVisualizacion;
    }

    public void setDuracionVisualizacion(Integer duracionVisualizacion) {
        this.duracionVisualizacion = duracionVisualizacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}