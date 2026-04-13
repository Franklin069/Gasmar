package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.dto.AcaoProcessoDTO;
import com.gasflow.gasflow.enums.TipoDocumento;
import com.gasflow.gasflow.enums.TipoPagamento;
import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.model.Pagamento;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.DocumentoRepository;
import com.gasflow.gasflow.repository.HistoricoProcessoRepository;
import com.gasflow.gasflow.service.DocumentoService;
import com.gasflow.gasflow.service.FileStorageService;
import com.gasflow.gasflow.service.ProcessoService;
import com.gasflow.gasflow.service.PagamentoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    private final ProcessoService processoService;
    private final HistoricoProcessoRepository historicoRepository;
    private final DocumentoService documentoService;
    private final PagamentoService pagamentoService;

    @Autowired
    private DocumentoRepository documentoRepository;
    @Autowired
    private FileStorageService fileStorageService;

    public ProcessoController(ProcessoService processoService,
                              HistoricoProcessoRepository historicoRepository,
                              DocumentoService documentoService, PagamentoService pagamentoService) {
        this.processoService = processoService;
        this.historicoRepository = historicoRepository;
        this.documentoService = documentoService;
        this.pagamentoService = pagamentoService;
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
        Double valorRestante = pagamentoService.calcularValorRestante(
                processo.getValorTotal(),
                processo.getId()
        );

        model.addAttribute("valorPago", valorPago);
        model.addAttribute("valorRestante", valorRestante);

        AcaoProcessoDTO acao = processoService.resolverAcao(processo, usuario);

        model.addAttribute("acao", acao);

        if (acao != null && "APROVAR_PAGAMENTO".equals(acao.getTipo())) {

            Pagamento pagamento = pagamentoService.buscarUltimoPagamentoPendente(processo.getId());

            model.addAttribute("valorPago", valorPago);
            model.addAttribute("valorSolicitado", pagamento.getValorSolicitado());
            model.addAttribute("adiantamento",
                    pagamento.getTipoPagamento() == TipoPagamento.ADIANTAMENTO);
            model.addAttribute("pagamentoId", pagamento.getId());
        }

        return "process-detail";
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
    ) {

        try {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");

            Processo processo = processoService.buscarPorId(processoId);

            // 🔹 Converter valor (R$ 1.234,56 → 1234.56)
            String valorLimpo = valor.replaceAll("[^0-9,]", "");

            if (valorLimpo.isBlank()) {
                throw new RuntimeException("Valor inválido");
            }

            Double valorConvertido = Double.parseDouble(valorLimpo.replace(",", "."));

            boolean isAdiantamento = adiantamento != null;

            // 🔹 Criar pagamento no banco
            processoService.solicitarPagamento(
                    processoId,
                    valorConvertido,
                    isAdiantamento,
                    usuario
            );

            // 🔹 Buscar último pagamento criado
            Pagamento pagamento = pagamentoService
                    .buscarUltimoPagamentoPendente(processoId);

            String identificador = processo.getIdentificador();
            String pastaPagamento = "pagamento_" + pagamento.getId();

            // 🔹 SALVAR ARQUIVOS
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

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Solicitação de pagamento enviada com sucesso."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

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

        doc.setUsuarioUpload(usuario); // 🔹 obrigatório
        doc.setTipo(tipo);             // 🔹 obrigatório
        doc.setDataUpload(LocalDateTime.now()); // 🔹 obrigatório

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
}