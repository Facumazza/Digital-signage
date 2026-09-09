package com.grenlus.signage.repository;

import com.grenlus.signage.entity.PlaylistContenido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistContenidoRepository
        extends JpaRepository<PlaylistContenido, Long> {

    List<PlaylistContenido> findByPlaylistIdOrderByOrdenAsc(Long playlistId);

}