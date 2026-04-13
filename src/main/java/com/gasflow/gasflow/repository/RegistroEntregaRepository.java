package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.RegistroEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroEntregaRepository extends JpaRepository<RegistroEntrega, Long> {
    Optional<RegistroEntrega> findTopByProcessoIdOrderByIdDesc(Long processoId);
}