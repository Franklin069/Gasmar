package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("usuarioLogado") != null) {
            return "redirect:/processos";
        }
        return "login";
    }

    @PostMapping("/login")
    public String fazerLogin(@RequestParam String login,
                             @RequestParam String senha,
                             HttpSession session,
                             Model model) {

        String loginLimpo = login.replaceAll("\\D", "");

        var usuarioOpt = usuarioRepository.findByLogin(loginLimpo);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("erro", "Login ou senha inválidos");
            return "login";
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            model.addAttribute("erro", "Login ou senha inválidos");
            return "login";
        }

        session.setAttribute("usuarioLogado", usuario);

        return "redirect:/processos";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}