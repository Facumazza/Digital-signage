package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByActivoTrue();

    boolean existsByCuit(String cuit);

    /** Para editar: el CUIT puede repetirse solo con el propio cliente. */
    boolean existsByCuitAndIdNot(String cuit, Long id);
}
