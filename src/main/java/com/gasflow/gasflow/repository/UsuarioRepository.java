package com.gasflow.gasflow.repository;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // LOGIN (CPF usado como login)
    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    // EMAIL
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // CPF
    Optional<Usuario> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    List<Usuario> findByPerfilOrderByNomeAsc(PerfilUsuario perfil);
}
