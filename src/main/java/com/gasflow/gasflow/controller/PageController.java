package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final UsuarioRepository usuarioRepository;

    public PageController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/pabs")
    public String pabs(HttpSession session) {
        if (obterUsuarioLogado(session) == null) {
            return "redirect:/";
        }
        return "pabs";
    }

    @GetMapping("/processo")
    public String processDetail(HttpSession session) {
        if (obterUsuarioLogado(session) == null) {
            return "redirect:/";
        }
        return "process-detail";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session) {
        if (obterUsuarioLogado(session) == null) {
            return "redirect:/";
        }
        return "notifications";
    }

    private Usuario obterUsuarioLogado(HttpSession session) {
        Usuario usuarioSessao = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioSessao == null || usuarioSessao.getLogin() == null) {
            return null;
        }

        Usuario usuarioAtualizado = usuarioRepository.findByLogin(usuarioSessao.getLogin())
                .orElse(usuarioSessao);

        session.setAttribute("usuarioLogado", usuarioAtualizado);
        return usuarioAtualizado;
    }
}
