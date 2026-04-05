package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.enums.PerfilUsuario;
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
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        model.addAttribute("processos", processoService.listarPorUsuario(usuarioLogado));
        model.addAttribute("usuarioLogado", usuarioLogado);

        return "dashboard";
    }

    @GetMapping("/novo")
    public String abrirFormulario(Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        if (usuarioLogado.getPerfil() != PerfilUsuario.DEMANDANTE) {
            return "redirect:/processos";
        }

        model.addAttribute("processo", new Processo());
        model.addAttribute("usuarioLogado", usuarioLogado);

        return "pabs";
    }

    @PostMapping("/salvar")
    public String salvarProcesso(@ModelAttribute Processo processo, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        if (usuarioLogado.getPerfil() != PerfilUsuario.DEMANDANTE) {
            return "redirect:/processos";
        }

        processoService.criarProcesso(processo, usuarioLogado.getId());
        return "redirect:/processos";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        model.addAttribute("processo", processoService.buscarPorId(id));
        model.addAttribute("usuarioLogado", usuarioLogado);

        return "process-detail";
    }

    @PostMapping("/{id}/autorizar")
    public String autorizarCompra(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.autorizarCompra(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/aprovar-pagamento")
    public String aprovarPagamento(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.aprovarPagamento(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/registrar-pagamento")
    public String registrarPagamento(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.registrarPagamento(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/receber-conforme")
    public String receberConforme(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.registrarRecebimento(id, usuarioLogado, true);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/receber-nao-conforme")
    public String receberNaoConforme(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.registrarRecebimento(id, usuarioLogado, false);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/validar-nf")
    public String validarNotaFiscal(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.validarNotaFiscal(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }
}