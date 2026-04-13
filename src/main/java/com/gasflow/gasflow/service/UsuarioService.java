package com.gasflow.gasflow.service;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario salvar(Usuario usuario) {
        if (usuario.getSenha() != null && !senhaJaCriptografada(usuario.getSenha())) {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }
        return usuarioRepository.save(usuario);
    }

    public boolean autenticar(String senhaDigitada, Usuario usuario) {
        if (usuario == null || senhaDigitada == null || usuario.getSenha() == null) {
            return false;
        }

        String senhaArmazenada = usuario.getSenha();
        if (senhaJaCriptografada(senhaArmazenada)) {
            return passwordEncoder.matches(senhaDigitada, senhaArmazenada);
        }

        boolean senhaCorreta = senhaDigitada.equals(senhaArmazenada);
        if (senhaCorreta) {
            usuario.setSenha(passwordEncoder.encode(senhaDigitada));
            usuarioRepository.save(usuario);
        }

        return senhaCorreta;
    }

    private boolean senhaJaCriptografada(String senha) {
        return senha.startsWith("$2a$") || senha.startsWith("$2b$") || senha.startsWith("$2y$");
    }
}
