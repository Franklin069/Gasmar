package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.TipoAquisicao;
import com.gasflow.gasflow.enums.TipoProcesso;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "processos")
@Data
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String identificador;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_processo", nullable = false)
    private TipoProcesso tipoProcesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aquisicao")
    private TipoAquisicao tipoAquisicao;

    @Column(name = "fornecedor_nome", length = 255)
    private String fornecedorNome;

    @Column(name = "fornecedor_cnpj", length = 18)
    private String fornecedorCnpj;

    @Column(name = "valor_negociado")
    private Double valorNegociado;

    @Column(name = "numero_nota_fiscal", length = 100)
    private String numeroNotaFiscal;

    @Column(name = "valor_nota_fiscal")
    private Double valorNotaFiscal;

    @Column(name = "justificativa_divergencia_nf", length = 1000)
    private String justificativaDivergenciaNf;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_atual", nullable = false)
    private StatusProcesso estadoAtual;

    @ManyToOne
    @JoinColumn(name = "setor_demandante_id", nullable = false)
    private Usuario setorDemandante;

    @ManyToOne
    @JoinColumn(name = "fiscal_id")
    private Usuario fiscal;
}
