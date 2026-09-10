package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Pantalla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PantallaRepository extends JpaRepository<Pantalla, Long> {

    Optional<Pantalla> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Pantalla> findBySucursalId(Long sucursalId);
}
