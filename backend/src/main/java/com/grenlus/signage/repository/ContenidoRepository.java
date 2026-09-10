package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Contenido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    List<Contenido> findByClienteIdAndActivoTrue(Long clienteId);
}
