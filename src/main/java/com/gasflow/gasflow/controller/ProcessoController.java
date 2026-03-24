package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.service.ProcessoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    private final ProcessoService processoService;

    public ProcessoController(ProcessoService processoService) {
        this.processoService = processoService;
    }

    @GetMapping
    public String listarProcessos(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        model.addAttribute("processos", processoService.listarTodos());
        return "dashboard";
    }

    @GetMapping("/novo")
    public String abrirFormulario(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        model.addAttribute("processo", new Processo());
        return "pabs";
    }

    @PostMapping("/salvar")
    public String salvarProcesso(@ModelAttribute Processo processo, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.criarProcesso(processo, usuarioLogado.getId());
        return "redirect:/processos";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        model.addAttribute("processo", processoService.buscarPorId(id));
        return "process-detail";
    }
}