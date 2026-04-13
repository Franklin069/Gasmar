package com.gasflow.gasflow.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "setores")
@Data
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 20)
    private String sigla;

    @Column(nullable = false)
    private Boolean ativo = true;
}