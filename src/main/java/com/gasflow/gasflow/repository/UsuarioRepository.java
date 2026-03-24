package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}