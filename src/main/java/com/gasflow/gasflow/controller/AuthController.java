package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.UsuarioRepository;
import com.gasflow.gasflow.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public AuthController(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
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

        var usuarioOpt = usuarioRepository.findByLogin(login);

        if (usuarioOpt.isEmpty() || !usuarioService.autenticar(senha, usuarioOpt.get())) {
            model.addAttribute("erro", "Login ou senha inválidos");
            return "login";
        }

        Usuario usuario = usuarioOpt.get();
        session.setAttribute("usuarioLogado", usuario);

        return "redirect:/processos";
    }

    @GetMapping({"/logout", "/sair"})
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
