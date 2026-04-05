package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.SetorRepository;
import com.gasflow.gasflow.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             SetorRepository setorRepository,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/novo")
    public String novoUsuario(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        model.addAttribute("usuario", new Usuario());
        model.addAttribute("setores", setorRepository.findAll());

        return "usuario-form";
    }

    @PostMapping("/salvar")
    public String salvarUsuario(@ModelAttribute Usuario usuario,
                                @RequestParam String confirmarSenha,
                                Model model,
                                HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        model.addAttribute("setores", setorRepository.findAll());

        String cpfLimpo = usuario.getCpf().replaceAll("\\D", "");
        usuario.setCpf(cpfLimpo);
        usuario.setLogin(cpfLimpo);

        if (!usuario.getSenha().equals(confirmarSenha)) {
            model.addAttribute("erro", "As senhas não coincidem.");
            return "usuario-form";
        }

        if (usuarioRepository.existsByCpf(usuario.getCpf())) {
            model.addAttribute("erro", "CPF já cadastrado.");
            return "usuario-form";
        }

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            model.addAttribute("erro", "E-mail já cadastrado.");
            return "usuario-form";
        }

        if (usuarioRepository.existsByLogin(usuario.getLogin())) {
            model.addAttribute("erro", "Já existe um usuário cadastrado com esse CPF/login.");
            return "usuario-form";
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        usuarioRepository.save(usuario);

        return "redirect:/processos";
    }
}