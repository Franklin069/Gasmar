package com.gasflow.gasflow.service;

import com.gasflow.gasflow.enums.StatusPagamento;
import com.gasflow.gasflow.model.Pagamento;
import com.gasflow.gasflow.repository.PagamentoRepository;
import org.springframework.stereotype.Service;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    public Double calcularValorPago(Long processoId) {
        return pagamentoRepository
                .somarValorPagoPorProcesso(processoId, StatusPagamento.PAGO);
    }

    public Double calcularValorRestante(Double valorTotal, Long processoId) {
        Double valorPago = calcularValorPago(processoId);
        return valorTotal - valorPago;
    }

    public Pagamento buscarUltimoPagamentoPendente(Long processoId) {
        return pagamentoRepository
                .findTopByProcessoIdAndStatusOrderByIdDesc(
                        processoId,
                        StatusPagamento.SOLICITADO
                )
                .orElse(null);
    }
}