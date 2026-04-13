package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.enums.Cargo;
import com.gasflow.gasflow.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);

    List<Usuario> findByCargo(Cargo cargo);
}