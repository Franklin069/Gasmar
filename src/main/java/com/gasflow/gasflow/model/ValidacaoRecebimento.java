package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.AcaoNaoConformidade;
import com.gasflow.gasflow.enums.StatusValidacaoRecebimento;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "validacoes_recebimento")
@Data
public class ValidacaoRecebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusValidacaoRecebimento status;

    @Column(name = "data_validacao", nullable = false)
    private LocalDateTime dataValidacao;

    @ManyToOne
    @JoinColumn(name = "registro_entrega_id", nullable = false)
    private RegistroEntrega registroEntrega;

    @ManyToOne
    @JoinColumn(name = "validado_por_id", nullable = false)
    private Usuario validadoPor;
}