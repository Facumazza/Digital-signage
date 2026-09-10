package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByClienteIdAndActivoTrue(Long clienteId);
}
