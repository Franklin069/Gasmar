package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    @Query("SELECT MAX(p.identificador) FROM Processo p WHERE p.identificador LIKE %:anoSufixo")
    String findMaxIdentificadorByAno(@Param("anoSufixo") String anoSufixo);

    long countByEstadoAtual(StatusProcesso estadoAtual);

    long countByEstadoAtualAndSetorDemandante(StatusProcesso estadoAtual, Usuario usuario);

    @EntityGraph(attributePaths = {"setorDemandante", "setorDemandante.setor", "fiscal", "fiscal.setor"})
    List<Processo> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"setorDemandante", "setorDemandante.setor", "fiscal", "fiscal.setor"})
    List<Processo> findBySetorDemandanteOrderByIdDesc(Usuario usuario);

    @EntityGraph(attributePaths = {"setorDemandante", "setorDemandante.setor", "fiscal", "fiscal.setor"})
    List<Processo> findTop10ByOrderByIdDesc();

    @EntityGraph(attributePaths = {"setorDemandante", "setorDemandante.setor", "fiscal", "fiscal.setor"})
    List<Processo> findTop10BySetorDemandanteOrderByIdDesc(Usuario usuario);

    @EntityGraph(attributePaths = {"setorDemandante", "setorDemandante.setor", "fiscal", "fiscal.setor"})
    @Query("SELECT p FROM Processo p " +
            "LEFT JOIN p.setorDemandante sd " +
            "LEFT JOIN sd.setor s " +
            "WHERE (:busca IS NULL OR TRIM(:busca) = '' OR LOWER(COALESCE(p.titulo, '')) LIKE LOWER(CONCAT('%', :busca, '%')) OR " +
            "LOWER(COALESCE(p.identificador, '')) LIKE LOWER(CONCAT('%', :busca, '%')) OR " +
            "LOWER(COALESCE(sd.nome, '')) LIKE LOWER(CONCAT('%', :busca, '%'))) AND " +
            "(:status IS NULL OR p.estadoAtual = :status) AND " +
            "(:tipoProcesso IS NULL OR p.tipoProcesso = :tipoProcesso) AND " +
            "(:setorId IS NULL OR s.id = :setorId) " +
            "ORDER BY p.id DESC")
    List<Processo> findWithFilters(@Param("busca") String busca,
                                   @Param("status") StatusProcesso status,
                                   @Param("tipoProcesso") com.gasflow.gasflow.enums.TipoProcesso tipoProcesso,
                                   @Param("setorId") Long setorId);
}
