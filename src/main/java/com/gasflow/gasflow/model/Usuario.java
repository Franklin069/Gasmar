package com.gasflow.gasflow.model;

import com.gasflow.gasflow.enums.PerfilUsuario;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false, length = 100)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuario perfil;

    @ManyToOne
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;    
}