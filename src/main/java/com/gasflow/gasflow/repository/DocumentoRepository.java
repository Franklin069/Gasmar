package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
}