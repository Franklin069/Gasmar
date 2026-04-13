package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.StatusGecont;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.TipoAquisicao;
import com.gasflow.gasflow.enums.TipoProcesso;
import jakarta.persistence.*;
import lombok.Data;

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

    @Column(name = "valor_total")
    private Double valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_processo")
    private TipoProcesso tipoProcesso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aquisicao")
    private TipoAquisicao tipoAquisicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_atual", nullable = false)
    private StatusProcesso estadoAtual;

    @ManyToOne
    @JoinColumn(name = "setor_demandante_id", nullable = false)
    private Usuario setorDemandante;

    @ManyToOne
    @JoinColumn(name = "fiscal_id")
    private Usuario fiscal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_gecont")
    private StatusGecont statusGecont;
}