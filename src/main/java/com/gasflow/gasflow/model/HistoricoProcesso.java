package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.StatusProcesso;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_processo")
@Data
public class HistoricoProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private StatusProcesso estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false)
    private StatusProcesso estadoNovo;

    @Column(name = "data_transicao", nullable = false)
    private LocalDateTime dataTransicao;

    @Column(length = 500)
    private String observacao;

    @ManyToOne
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}