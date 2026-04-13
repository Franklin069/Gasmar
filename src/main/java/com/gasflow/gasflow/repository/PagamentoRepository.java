package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.enums.StatusPagamento;
import com.gasflow.gasflow.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("""
    SELECT COALESCE(SUM(p.valorPago), 0)
    FROM Pagamento p
    WHERE p.processo.id = :processoId
    AND p.status = :status
""")
    Double somarValorPagoPorProcesso(
            @Param("processoId") Long processoId,
            @Param("status") StatusPagamento status
    );

    Optional<Pagamento> findTopByProcessoIdAndStatusOrderByIdDesc(
                Long processoId,
                StatusPagamento status
        );
}