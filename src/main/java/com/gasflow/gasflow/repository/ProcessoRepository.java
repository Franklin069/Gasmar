package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    @Query("SELECT MAX(p.identificador) FROM Processo p WHERE p.identificador LIKE %:anoSufixo")
    String findMaxIdentificadorByAno(@Param("anoSufixo") String anoSufixo);

    long countByEstadoAtual(StatusProcesso estadoAtual);

    List<Processo> findAllByOrderByIdDesc();

    List<Processo> findBySetorDemandanteOrderByIdDesc(Usuario usuario);
}