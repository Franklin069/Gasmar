package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Processo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {
}