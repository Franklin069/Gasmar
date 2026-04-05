package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    List<Processo> findBySetorDemandante(Usuario usuario);

}