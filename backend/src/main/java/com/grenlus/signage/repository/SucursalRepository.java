package com.grenlus.signage.repository;

import com.grenlus.signage.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepository
        extends JpaRepository<Sucursal, Long> {
}