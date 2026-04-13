package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.enums.AcaoNaoConformidade;
import com.gasflow.gasflow.enums.PerfilUsuario;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.TipoAquisicao;
import com.gasflow.gasflow.enums.TipoPagamento;
import com.gasflow.gasflow.enums.TipoProcesso;
import com.gasflow.gasflow.model.Documento;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

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
        Usuario usuarioLogado = obterUsuarioLogado(session);
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
        Usuario usuarioLogado = obterUsuarioLogado(session);
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
        Usuario usuarioLogado = obterUsuarioLogado(session);
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
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }
        if (usuarioLogado.getPerfil() != PerfilUsuario.DEMANDANTE) {
            return "redirect:/processos";
        }

        processoService.criarProcesso(processo, usuarioLogado.getId(), pabsFile, memorialFile, mapaCotacaoFile, propostasFile, outrosFile);
        return "redirect:/processos";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model, HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        Processo processo = processoService.buscarPorId(id);
        model.addAttribute("processo", processo);
        model.addAttribute("documentos", processoService.listarDocumentosPorProcesso(id));
        model.addAttribute("pagamento", processoService.buscarUltimoPagamento(id));
        model.addAttribute("registroEntrega", processoService.buscarUltimoRegistroEntrega(id));
        model.addAttribute("validacaoRecebimento", processoService.buscarUltimaValidacaoRecebimento(id));
        model.addAttribute("historico", processoService.listarHistorico(id));
        model.addAttribute("fiscais", processoService.listarPossiveisFiscais());
        model.addAttribute("gestores", processoService.listarUsuariosPorPerfil(PerfilUsuario.GESTOR));
        model.addAttribute("tipoPagamentoOptions", TipoPagamento.values());
        model.addAttribute("acaoNaoConformidadeOptions", AcaoNaoConformidade.values());
        model.addAttribute("usuarioLogado", usuarioLogado);
        model.addAttribute("resumoProcesso", processoService.gerarResumoProcesso(processo));
        return "process-detail";
    }

    @PostMapping("/{id}/analisar")
    public String analisarCompra(@PathVariable Long id,
                                 @RequestParam TipoAquisicao tipoAquisicao,
                                 @RequestParam String fornecedorNome,
                                 @RequestParam String fornecedorCnpj,
                                 @RequestParam Double valorNegociado,
                                 @RequestParam(required = false) Long fiscalId,
                                 HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.analisarCompra(id, usuarioLogado, tipoAquisicao, fornecedorNome, fornecedorCnpj, valorNegociado, fiscalId);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/autorizar")
    public String autorizarCompra(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.autorizarCompra(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/rejeitar")
    public String rejeitarProcesso(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.rejeitarProcesso(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/solicitar-pagamento")
    public String solicitarPagamento(@PathVariable Long id,
                                     @RequestParam TipoPagamento tipoPagamento,
                                     @RequestParam Double valorSolicitado,
                                     @RequestParam String dataSolicitacao,
                                     HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.solicitarPagamento(id, usuarioLogado, tipoPagamento, valorSolicitado, LocalDate.parse(dataSolicitacao));
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/aprovar-pagamento")
    public String aprovarPagamento(@PathVariable Long id, HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.aprovarPagamento(id, usuarioLogado);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/registrar-pagamento")
    public String registrarPagamento(@PathVariable Long id,
                                     @RequestParam Double valorPago,
                                     @RequestParam String dataPagamento,
                                     @RequestParam(value = "comprovantePagamentoFile", required = false) MultipartFile comprovantePagamentoFile,
                                     HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.registrarPagamento(id, usuarioLogado, valorPago, LocalDate.parse(dataPagamento), comprovantePagamentoFile);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/receber")
    public String registrarRecebimento(@PathVariable Long id,
                                       @RequestParam String nomeRecebedor,
                                       @RequestParam boolean conforme,
                                       @RequestParam(required = false) String descricaoProblema,
                                       @RequestParam(required = false) AcaoNaoConformidade acaoNaoConformidade,
                                       @RequestParam(value = "evidenciaRecebimentoFile", required = false) MultipartFile evidenciaRecebimentoFile,
                                       HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.registrarRecebimento(id, usuarioLogado, nomeRecebedor, conforme, descricaoProblema, acaoNaoConformidade, evidenciaRecebimentoFile);
        return "redirect:/processos/" + id;
    }

    @PostMapping("/{id}/validar-nf")
    public String validarNotaFiscal(@PathVariable Long id,
                                    @RequestParam String numeroNotaFiscal,
                                    @RequestParam Double valorNotaFiscal,
                                    @RequestParam(required = false) String justificativaDivergencia,
                                    @RequestParam(value = "notaFiscalFile", required = false) MultipartFile notaFiscalFile,
                                    HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        processoService.validarNotaFiscal(id, usuarioLogado, numeroNotaFiscal, valorNotaFiscal, justificativaDivergencia, notaFiscalFile);
        return "redirect:/processos/" + id;
    }

    @GetMapping("/{processoId}/documentos/{documentoId}/visualizar")
    public ResponseEntity<Resource> visualizarDocumento(@PathVariable Long processoId,
                                                        @PathVariable Long documentoId,
                                                        HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/").build();
        }

        Documento documento = processoService.buscarDocumentoDoProcesso(processoId, documentoId);
        return montarRespostaArquivo(documento, false);
    }

    @GetMapping("/{processoId}/documentos/{documentoId}/download")
    public ResponseEntity<Resource> baixarDocumento(@PathVariable Long processoId,
                                                    @PathVariable Long documentoId,
                                                    HttpSession session) {
        Usuario usuarioLogado = obterUsuarioLogado(session);
        if (usuarioLogado == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/").build();
        }

        Documento documento = processoService.buscarDocumentoDoProcesso(processoId, documentoId);
        return montarRespostaArquivo(documento, true);
    }

    private Usuario obterUsuarioLogado(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogado");
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
