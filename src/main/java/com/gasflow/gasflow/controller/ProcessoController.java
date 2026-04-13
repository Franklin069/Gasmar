package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.dto.AcaoProcessoDTO;
import com.gasflow.gasflow.enums.*;
import com.gasflow.gasflow.model.*;
import com.gasflow.gasflow.repository.*;
import com.gasflow.gasflow.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    private final ProcessoService processoService;
    private final HistoricoProcessoRepository historicoRepository;
    private final DocumentoService documentoService;
    private final PagamentoService pagamentoService;
    private final RegistroEntregaService registroEntregaService;
    private final ValidacaoRecebimentoRepository validacaoRecebimentoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;

    @Autowired
    private DocumentoRepository documentoRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ProcessoRepository processoRepository;

    public ProcessoController(ProcessoService processoService,
                              HistoricoProcessoRepository historicoRepository,
                              DocumentoService documentoService, PagamentoService pagamentoService, RegistroEntregaService registroEntregaService, ValidacaoRecebimentoRepository validacaoRecebimentoRepository, PagamentoRepository pagamentoRepository, UsuarioRepository usuarioRepository, SetorRepository setorRepository) {
        this.processoService = processoService;
        this.historicoRepository = historicoRepository;
        this.documentoService = documentoService;
        this.pagamentoService = pagamentoService;
        this.registroEntregaService = registroEntregaService;
        this.validacaoRecebimentoRepository = validacaoRecebimentoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
    }

    @GetMapping
    public String dashboard(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            return "redirect:/";
        }

        long qtdAguardandoAnalise = processoRepository.countByEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE);
        long qtdEmTransito = processoRepository.countByEstadoAtual(StatusProcesso.AGUARDANDO_RECEBIMENTO_EXECUCAO);
        long qtdPagamento = processoRepository.countByEstadoAtual(StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO);

        model.addAttribute("qtdAguardandoAnalise", qtdAguardandoAnalise);
        model.addAttribute("qtdEmTransito", qtdEmTransito);
        model.addAttribute("qtdPagamento", qtdPagamento);

        model.addAttribute("processos", processoService.listarTodos());

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

        return "all-process";
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
    public String salvarProcesso(
            @ModelAttribute Processo processo,
            @RequestParam(value = "docMemorial", required = false) MultipartFile docMemorial,
            @RequestParam("docPabs") MultipartFile docPabs, // Único arquivo obrigatório no front
            @RequestParam(value = "docMapa", required = false) MultipartFile docMapa,
            @RequestParam(value = "docPropostas", required = false) MultipartFile docPropostas,
            @RequestParam(value = "docOutros", required = false) MultipartFile[] docOutros,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/";
        }

        Processo processoSalvo = processoService.criarProcesso(processo, usuarioLogado);
        String identificador = processoSalvo.getIdentificador();

        if (docPabs != null && !docPabs.isEmpty()) {
            String caminhoPabs = fileStorageService.salvar(
                    docPabs, identificador, "requisicao", "pabs"
            );
            salvarDocumento(docPabs, caminhoPabs, processoSalvo, usuarioLogado, TipoDocumento.PABS);
        }

        if (docMemorial != null && !docMemorial.isEmpty()) {
            String caminhoMemorial = fileStorageService.salvar(
                    docMemorial, identificador, "requisicao", "memorial"
            );
            salvarDocumento(docMemorial, caminhoMemorial, processoSalvo, usuarioLogado, TipoDocumento.MEMORIAL_DESCRITIVO);
        }

        if (docMapa != null && !docMapa.isEmpty()) {
            String caminhoMapa = fileStorageService.salvar(
                    docMapa, identificador, "requisicao", "mapa_cotacao"
            );
            salvarDocumento(docMapa, caminhoMapa, processoSalvo, usuarioLogado, TipoDocumento.MAPA_COTACAO);
        }

        if (docPropostas != null && !docPropostas.isEmpty()) {
            String caminhoPropostas = fileStorageService.salvar(
                    docPropostas, identificador, "requisicao", "propostas"
            );
            salvarDocumento(docPropostas, caminhoPropostas, processoSalvo, usuarioLogado, TipoDocumento.PROPOSTA);
        }

        if (docOutros != null) {
            for (MultipartFile file : docOutros) {
                if (file != null && !file.isEmpty()) {
                    String caminhoOutro = fileStorageService.salvar(
                            file, identificador, "requisicao", "outros"
                    );
                    salvarDocumento(file, caminhoOutro, processoSalvo, usuarioLogado, TipoDocumento.OUTRO);
                }
            }
        }

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Requisição registrada com sucesso! Protocolo gerado: " + identificador
        );

        return "redirect:/processos";
    }

    @GetMapping("/{id}")
    public String detalharProcesso(@PathVariable Long id, Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/";
        }

        Processo processo = processoService.buscarPorId(id);

        model.addAttribute("processo", processo);

        model.addAttribute("historicos",
                historicoRepository.findByProcessoId(id)
        );

        model.addAttribute("documentos",
                documentoService.listarPorProcesso(id)
        );

        Double valorPago = pagamentoService.calcularValorPago(processo.getId());

        Double valorTotalSeguro = processo.getValorTotal() != null ? processo.getValorTotal() : 0.0;

        Double valorRestante = pagamentoService.calcularValorRestante(
                valorTotalSeguro,
                processo.getId()
        );

        model.addAttribute("valorPago", valorPago);
        model.addAttribute("valorRestante", valorRestante);

        model.addAttribute("usuarios",
                usuarioRepository.findByCargo(Cargo.FISCAL)
        );

        List<AcaoProcessoDTO> acoes = processoService.resolverAcoes(processo, usuario);
        model.addAttribute("acoes", acoes);

        Pagamento pagamento = pagamentoService.buscarUltimoPagamentoAutorizado(processo.getId());

        if (pagamento != null) {
            model.addAttribute("pagamentoId", pagamento.getId());
        }

        boolean temAprovacao = acoes.stream()
                .anyMatch(a -> "APROVAR_PAGAMENTO".equals(a.getTipo()));

        if (temAprovacao) {

            Pagamento pagamento_aprovado = pagamentoService.buscarUltimoPagamentoPendente(processo.getId());

            model.addAttribute("valorPago", valorPago);
            model.addAttribute("valorSolicitado", pagamento_aprovado.getValorSolicitado());
            model.addAttribute("adiantamento",
                    pagamento_aprovado.getTipoPagamento() == TipoPagamento.ADIANTAMENTO);
            model.addAttribute("pagamentoId", pagamento_aprovado.getId());
        }

        return "process-detail";
    }

    @PostMapping("/{id}/conduzir")
    public String conduzirCompra(
            @PathVariable Long id,
            @RequestParam String valorTotal,
            @RequestParam TipoAquisicao tipoAquisicao,
            @RequestParam TipoProcesso tipoProcesso,
            @RequestParam(required = false) Long fiscalId,
            @RequestParam(required = false) Boolean solicitarAdiantamento,
            @RequestParam(value = "autorizacaoCompra", required = false) MultipartFile autorizacaoCompra,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        String valorLimpo = valorTotal.replaceAll("[^0-9,]", "");
        if (valorLimpo.isBlank()) {
            throw new RuntimeException("Valor inválido");
        }
        Double valorConvertido = Double.parseDouble(valorLimpo.replace(",", "."));

        boolean isAdiantamento = Boolean.TRUE.equals(solicitarAdiantamento);

        Processo processo = processoService.conduzirProcesso(
                id, valorConvertido, tipoAquisicao, tipoProcesso, fiscalId, isAdiantamento, usuario
        );

        if (autorizacaoCompra != null && !autorizacaoCompra.isEmpty()) {
            String caminhoAutorizacao = fileStorageService.salvar(
                    autorizacaoCompra,
                    processo.getIdentificador(),
                    "autorizacoes",
                    "etapa_conducao",
                    "autorizacao_compra"
            );
            salvarDocumento(autorizacaoCompra, caminhoAutorizacao, processo, usuario, TipoDocumento.AUTORIZACAO_COMPRA);
        }

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Compra conduzida com sucesso.");
        return "redirect:/processos/" + id;
    }


    @PostMapping("/{id}/analise-fiscal")
    public String analiseFiscal(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean solicitarAdiantamento,
            @RequestParam("autorizacaoCompra") MultipartFile autorizacaoCompra,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        boolean isAdiantamento = Boolean.TRUE.equals(solicitarAdiantamento);

        Processo processo = processoService.analiseFiscal(id, isAdiantamento, usuario);

        if (autorizacaoCompra != null && !autorizacaoCompra.isEmpty()) {
            String caminhoAutorizacao = fileStorageService.salvar(
                    autorizacaoCompra,
                    processo.getIdentificador(),
                    "autorizacoes",
                    "etapa_fiscal",
                    "autorizacao_fiscal"
            );
            salvarDocumento(autorizacaoCompra, caminhoAutorizacao, processo, usuario, TipoDocumento.AUTORIZACAO_COMPRA);
        }

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Análise do fiscal concluída com sucesso.");
        return "redirect:/processos/" + id;
    }

    @PostMapping("/solicitar-pagamento")
    public String solicitarPagamento(
            @RequestParam Long processoId,
            @RequestParam String valor,
            @RequestParam(required = false) String adiantamento,
            @RequestParam("notaFiscal") MultipartFile notaFiscal,
            @RequestParam("boleto") MultipartFile boleto,
            @RequestParam(value = "outrosDocumentos", required = false) MultipartFile[] outros,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

            Processo processo = processoService.buscarPorId(processoId);

            String valorLimpo = valor.replaceAll("[^0-9,]", "");

            if (valorLimpo.isBlank()) {
                throw new RuntimeException("Valor inválido");
            }

            Double valorConvertido = Double.parseDouble(valorLimpo.replace(",", "."));

            boolean isAdiantamento = adiantamento != null;

            processoService.solicitarPagamento(
                    processoId,
                    valorConvertido,
                    isAdiantamento,
                    usuario
            );

            Pagamento pagamento = pagamentoService
                    .buscarUltimoPagamentoPendente(processoId);

            String identificador = processo.getIdentificador();
            String pastaPagamento = "pagamento_" + pagamento.getId();

            String caminhoNota = fileStorageService.salvar(
                    notaFiscal,
                    processo.getIdentificador(),
                    "pagamentos",
                    "pagamento_" + pagamento.getId(),
                    "nota_fiscal"
            );

            salvarDocumento(notaFiscal, caminhoNota, processo, usuario, TipoDocumento.NOTA_FISCAL);

            String caminhoBoleto = fileStorageService.salvar(
                    boleto,
                    processo.getIdentificador(),
                    "pagamentos",
                    "pagamento_" + pagamento.getId(),
                    "boleto"
            );

            salvarDocumento(boleto, caminhoBoleto, processo, usuario, TipoDocumento.BOLETO);

            if (outros != null) {
                for (MultipartFile file : outros) {

                    if (file != null && !file.isEmpty()) {

                        String caminhoOutro = fileStorageService.salvar(
                                file,
                                processo.getIdentificador(),
                                "pagamentos",
                                "pagamento_" + pagamento.getId(),
                                "outros"
                        );

                        salvarDocumento(file, caminhoOutro, processo, usuario, TipoDocumento.OUTRO);
                    }
                }
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Solicitação de pagamento enviada com sucesso."
            );

        return "redirect:/processos/" + processoId;
    }

    private void salvarDocumento(
            MultipartFile file,
            String caminho,
            Processo processo,
            Usuario usuario,
            TipoDocumento tipo
    ) {

        Documento doc = new Documento();

        doc.setNomeArquivo(file.getOriginalFilename());
        doc.setCaminhoArquivo(caminho);
        doc.setProcesso(processo);

        doc.setUsuarioUpload(usuario);
        doc.setTipo(tipo);
        doc.setDataUpload(LocalDateTime.now());

        documentoRepository.save(doc);
    }

    @PostMapping("/aprovar-pagamento")
    public String aprovarPagamento(
            @RequestParam Long processoId,
            @RequestParam Long pagamentoId,
            @RequestParam String acao,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        if ("aprovar".equals(acao)) {

            Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

            processoService.aprovarPagamento(processoId, pagamentoId, usuarioLogado);

            redirectAttributes.addFlashAttribute("mensagemSucesso",
                    "Pagamento aprovado e encaminhado para execução.");
        }

        return "redirect:/processos/" + processoId;
    }

    @PostMapping("/executar-pagamento")
    public String executarPagamento(
            @RequestParam Long processoId,
            @RequestParam Long pagamentoId,
            @RequestParam("comprovante") MultipartFile comprovante,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        Processo processo = processoService.buscarPorId(processoId);

        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));

        Double valorPago = pagamento.getValorSolicitado();

        if (comprovante != null && !comprovante.isEmpty()) {

            String caminho = fileStorageService.salvar(
                    comprovante,
                    processo.getIdentificador(),
                    "pagamentos",
                    "pagamento_" + pagamento.getId(),
                    "comprovantes_pagamento"
            );

            salvarDocumento(
                    comprovante,
                    caminho,
                    processo,
                    usuario,
                    TipoDocumento.COMPROVANTE_PAGAMENTO
            );
        }

        processoService.executarPagamento(
                processo,
                pagamento,
                valorPago,
                usuario
        );

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Pagamento realizado com sucesso."
        );

        return "redirect:/processos/" + processoId;
    }

    @PostMapping("/confirmar-execucao")
    public String confirmarExecucao(
            @RequestParam Long processoId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        processoService.confirmarExecucao(processoId, usuario);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Execução confirmada com sucesso."
        );

        return "redirect:/processos/" + processoId;
    }

    @PostMapping("/validar-material")
    public String validarMaterial(
            @RequestParam Long processoId,
            @RequestParam("fotos") MultipartFile[] fotos,
            @RequestParam String acao,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        Processo processo = processoService.buscarPorId(processoId);

        RegistroEntrega registroEntrega = registroEntregaService
                .buscarUltimoPorProcesso(processoId);

        if (registroEntrega == null) {
            throw new RuntimeException("Registro de entrega não encontrado");
        }

        StatusValidacaoRecebimento status =
                "aprovar".equals(acao)
                        ? StatusValidacaoRecebimento.CONFORME
                        : StatusValidacaoRecebimento.NAO_CONFORME;

        ValidacaoRecebimento validacao = new ValidacaoRecebimento();
        validacao.setStatus(status);
        validacao.setDataValidacao(LocalDateTime.now());
        validacao.setRegistroEntrega(registroEntrega);
        validacao.setValidadoPor(usuario);

        validacaoRecebimentoRepository.save(validacao);

        if (fotos != null) {
            for (MultipartFile foto : fotos) {

                if (foto == null || foto.isEmpty()) continue;

                String caminho = fileStorageService.salvar(
                        foto,
                        processo.getIdentificador(),
                        "recebimento",
                        null,
                        "foto_" + System.currentTimeMillis()
                );

                salvarDocumento(
                        foto,
                        caminho,
                        processo,
                        usuario,
                        TipoDocumento.EVIDENCIA_RECEBIMENTO
                );
            }
        }
        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        if (status == StatusValidacaoRecebimento.CONFORME) {
            processo.setEstadoAtual(StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO);
        } else {
            processo.setEstadoAtual(StatusProcesso.RECEBIDO_NAO_CONFORME);
        }

        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(processo.getEstadoAtual());
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao(
                status == StatusValidacaoRecebimento.CONFORME
                        ? "Material validado como conforme"
                        : "Material reprovado (não conforme)"
        );
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoRepository.save(historico);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                status == StatusValidacaoRecebimento.CONFORME
                        ? "Material validado com sucesso."
                        : "Não conformidade registrada."
        );

        return "redirect:/processos/" + processoId;
    }

    @PostMapping("/{id}/gecont/concordar")
    public String validarNotaFiscal(
            @PathVariable Long id,
            @RequestParam String observacao,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        Processo processo = processoService.buscarPorId(id);

        processoService.validarNotaFiscalGecont(processo, usuario, observacao);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Nota fiscal validada com sucesso."
        );

        return "redirect:/processos/" + id;
    }

    @PostMapping("/inconformidade")
    public String registrarInconformidade(
            @RequestParam Long processoId,
            @RequestParam String motivo,
            HttpSession session
    ) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

        Processo processo = processoService.buscarPorId(processoId);

        processoService.registrarInconformidade(processo, usuario, motivo);

        return "redirect:/processos/" + processoId;
    }
}