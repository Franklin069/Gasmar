package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.HistoricoProcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoProcessoRepository extends JpaRepository<HistoricoProcesso, Long> {
    List<HistoricoProcesso> findByProcessoIdOrderByDataTransicaoAsc(Long processoId);
}
