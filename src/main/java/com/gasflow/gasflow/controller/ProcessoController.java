package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.TipoProcesso;
import com.gasflow.gasflow.enums.PerfilUsuario;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.SetorRepository;
import com.gasflow.gasflow.service.ProcessoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    private final ProcessoService processoService;
    private final SetorRepository setorRepository;

    public ProcessoController(ProcessoService processoService, SetorRepository setorRepository) {
        this.processoService = processoService;
        this.setorRepository = setorRepository;
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

    @GetMapping("/todos")
    public String listarTodosProcessos(@RequestParam(required = false) String busca,
                                       @RequestParam(required = false) StatusProcesso status,
                                       @RequestParam(required = false) TipoProcesso tipoProcesso,
                                       @RequestParam(required = false) Long setorId,
                                       Model model,
                                       HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        model.addAttribute("processos", processoService.listarComFiltros(usuarioLogado, busca, status, tipoProcesso, setorId));
        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("statusOptions", StatusProcesso.values());
        model.addAttribute("tipoProcessoOptions", TipoProcesso.values());
        model.addAttribute("setores", setorRepository.findAll());
        model.addAttribute("busca", busca);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("tipoProcessoSelecionado", tipoProcesso);
        model.addAttribute("setorSelecionado", setorId);

        return "all-processes";
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
    public String salvarProcesso(@ModelAttribute Processo processo,
                                 @RequestParam("pabsFile") MultipartFile pabsFile,
                                 @RequestParam(value = "memorialFile", required = false) MultipartFile memorialFile,
                                 @RequestParam(value = "mapaCotacaoFile", required = false) MultipartFile mapaCotacaoFile,
                                 @RequestParam(value = "propostasFile", required = false) MultipartFile propostasFile,
                                 @RequestParam(value = "outrosFile", required = false) MultipartFile outrosFile,
                                 HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        if (usuarioLogado.getPerfil() != PerfilUsuario.DEMANDANTE) {
            return "redirect:/processos";
        }

        processoService.criarProcesso(
                processo,
                usuarioLogado.getId(),
                pabsFile,
                memorialFile,
                mapaCotacaoFile,
                propostasFile,
                outrosFile
        );
        return "redirect:/processos";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        model.addAttribute("processo", processoService.buscarPorId(id));
        model.addAttribute("documentos", processoService.listarDocumentosPorProcesso(id));
        model.addAttribute("usuarioLogado", usuarioLogado);

        return "process-detail";
    }

    @GetMapping("/{processoId}/documentos/{documentoId}/visualizar")
    public ResponseEntity<Resource> visualizarDocumento(@PathVariable Long processoId,
                                                        @PathVariable Long documentoId,
                                                        HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/")
                    .build();
        }

        Documento documento = processoService.buscarDocumentoDoProcesso(processoId, documentoId);
        return montarRespostaArquivo(documento, false);
    }

    @GetMapping("/{processoId}/documentos/{documentoId}/download")
    public ResponseEntity<Resource> baixarDocumento(@PathVariable Long processoId,
                                                    @PathVariable Long documentoId,
                                                    HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/")
                    .build();
        }

        Documento documento = processoService.buscarDocumentoDoProcesso(processoId, documentoId);
        return montarRespostaArquivo(documento, true);
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

    @PostMapping("/{id}/rejeitar")
    public String rejeitarProcesso(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.rejeitarProcesso(id, usuarioLogado);
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

    private ResponseEntity<Resource> montarRespostaArquivo(Documento documento, boolean download) {
        try {
            Path caminhoArquivo = Paths.get(documento.getCaminhoArquivo()).normalize();
            Resource resource = new UrlResource(caminhoArquivo.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Arquivo nao encontrado no servidor");
            }

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String nomeArquivo = documento.getNomeArquivo();
            String nomeArquivoLower = nomeArquivo.toLowerCase();

            if (nomeArquivoLower.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (nomeArquivoLower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (nomeArquivoLower.endsWith(".jpg") || nomeArquivoLower.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                    .filename(nomeArquivo)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Nao foi possivel abrir o arquivo solicitado", e);
        }
    }
}
