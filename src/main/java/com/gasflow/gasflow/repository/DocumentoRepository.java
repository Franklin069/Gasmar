package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByProcessoIdOrderByDataUploadDesc(Long processoId);
}
