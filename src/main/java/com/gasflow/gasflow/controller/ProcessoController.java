package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.service.ProcessoService;
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

    @GetMapping("/novo")
    public String abrirFormulario(Model model) {
        model.addAttribute("processo", new Processo());
        return "pabs";
    }

    @PostMapping("/salvar")
    public String salvarProcesso(@ModelAttribute Processo processo) {
        Long usuarioFixoId = 1L;
        processoService.criarProcesso(processo, usuarioFixoId);
        return "redirect:/processos";
    }

    @GetMapping
    public String listarProcessos(Model model) {
        model.addAttribute("processos", processoService.listarTodos());
        return "dashboard";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model) {
        model.addAttribute("processo", processoService.buscarPorId(id));
        return "process-detail";
    }
}