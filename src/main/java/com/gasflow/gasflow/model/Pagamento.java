package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.StatusPagamento;
import com.gasflow.gasflow.enums.TipoPagamento;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "pagamentos")
@Data
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false)
    private TipoPagamento tipoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status;

    @Column(name = "valor_solicitado", nullable = false)
    private Double valorSolicitado;

    @Column(name = "valor_pago")
    private Double valorPago;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDate dataSolicitacao;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @ManyToOne
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @ManyToOne
    @JoinColumn(name = "autorizado_por_id")
    private Usuario autorizadoPor;
}