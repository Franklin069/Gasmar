package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Pagamento findTopByProcessoIdOrderByIdDesc(Long processoId);
}
