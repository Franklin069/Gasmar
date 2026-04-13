package com.gasflow.gasflow.service;

import com.gasflow.gasflow.enums.AcaoNaoConformidade;
import com.gasflow.gasflow.enums.PerfilUsuario;
import com.gasflow.gasflow.enums.StatusPagamento;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.StatusValidacaoRecebimento;
import com.gasflow.gasflow.enums.TipoAquisicao;
import com.gasflow.gasflow.enums.TipoDocumento;
import com.gasflow.gasflow.enums.TipoPagamento;
import com.gasflow.gasflow.enums.TipoProcesso;
import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.model.HistoricoProcesso;
import com.gasflow.gasflow.model.Pagamento;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.RegistroEntrega;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.model.ValidacaoRecebimento;
import com.gasflow.gasflow.repository.DocumentoRepository;
import com.gasflow.gasflow.repository.HistoricoProcessoRepository;
import com.gasflow.gasflow.repository.PagamentoRepository;
import com.gasflow.gasflow.repository.ProcessoRepository;
import com.gasflow.gasflow.repository.RegistroEntregaRepository;
import com.gasflow.gasflow.repository.UsuarioRepository;
import com.gasflow.gasflow.repository.ValidacaoRecebimentoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcessoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoService.class);
    private static final double DIVERGENCIA_TOLERANCIA = 0.01;

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final RegistroEntregaRepository registroEntregaRepository;
    private final ValidacaoRecebimentoRepository validacaoRecebimentoRepository;
    private final HistoricoProcessoRepository historicoProcessoRepository;
    private final Path uploadDir;

    public ProcessoService(ProcessoRepository processoRepository,
                           UsuarioRepository usuarioRepository,
                           DocumentoRepository documentoRepository,
                           PagamentoRepository pagamentoRepository,
                           RegistroEntregaRepository registroEntregaRepository,
                           ValidacaoRecebimentoRepository validacaoRecebimentoRepository,
                           HistoricoProcessoRepository historicoProcessoRepository,
                           @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoRepository = documentoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.registroEntregaRepository = registroEntregaRepository;
        this.validacaoRecebimentoRepository = validacaoRecebimentoRepository;
        this.historicoProcessoRepository = historicoProcessoRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public Processo criarProcesso(Processo processo,
                                  Long usuarioId,
                                  MultipartFile pabsFile,
                                  MultipartFile memorialFile,
                                  MultipartFile mapaCotacaoFile,
                                  MultipartFile propostasFile,
                                  MultipartFile outrosFile) {
        Usuario usuario = buscarUsuario(usuarioId);

        if (pabsFile == null || pabsFile.isEmpty()) {
            throw new RuntimeException("O arquivo PABS e obrigatorio");
        }

        processo.setIdentificador(gerarIdentificador());
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE_GERAF);
        processo.setSetorDemandante(usuario);
        processo.setTipoAquisicao(null);
        processo.setFornecedorNome(null);
        processo.setFornecedorCnpj(null);
        processo.setValorNegociado(null);
        processo.setNumeroNotaFiscal(null);
        processo.setValorNotaFiscal(null);
        processo.setJustificativaDivergenciaNf(null);
        processo.setFiscal(null);
        processo.setDataEncerramento(null);

        Processo processoSalvo = processoRepository.save(processo);

        salvarDocumento(processoSalvo, usuario, pabsFile, TipoDocumento.PABS);
        salvarDocumento(processoSalvo, usuario, memorialFile, TipoDocumento.MEMORIAL_DESCRITIVO);
        salvarDocumento(processoSalvo, usuario, mapaCotacaoFile, TipoDocumento.MAPA_COTACAO);
        salvarDocumento(processoSalvo, usuario, propostasFile, TipoDocumento.PROPOSTA);
        salvarDocumento(processoSalvo, usuario, outrosFile, TipoDocumento.OUTRO);

        registrarHistorico(processoSalvo, usuario, null, StatusProcesso.AGUARDANDO_ANALISE_GERAF, "Processo aberto com upload inicial da PABS.");
        return processoSalvo;
    }

    public List<Processo> listarTodos() {
        return processoRepository.findAllByOrderByIdDesc();
    }

    public List<Processo> listarPorUsuario(Usuario usuario) {
        if (usuario.getPerfil() == PerfilUsuario.DEMANDANTE) {
            return processoRepository.findBySetorDemandanteOrderByIdDesc(usuario);
        }
        return processoRepository.findAllByOrderByIdDesc();
    }

    public List<Processo> listarComFiltros(Usuario usuario,
                                           String busca,
                                           StatusProcesso status,
                                           TipoProcesso tipoProcesso,
                                           Long setorId) {
        return listarPorUsuario(usuario).stream()
                .filter(processo -> filtroBusca(processo, busca))
                .filter(processo -> status == null || processo.getEstadoAtual() == status)
                .filter(processo -> tipoProcesso == null || processo.getTipoProcesso() == tipoProcesso)
                .filter(processo -> filtroSetor(usuario, processo, setorId))
                .collect(Collectors.toList());
    }

    public Processo buscarPorId(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Processo nao encontrado"));
    }

    public Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }

    public List<Usuario> listarUsuariosPorPerfil(PerfilUsuario perfil) {
        return usuarioRepository.findByPerfilOrderByNomeAsc(perfil);
    }

    public List<Usuario> listarPossiveisFiscais() {
        return usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getPerfil() != PerfilUsuario.ADMIN)
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .collect(Collectors.toList());
    }

    public List<Documento> listarDocumentosPorProcesso(Long processoId) {
        return documentoRepository.findByProcessoIdOrderByDataUploadDesc(processoId);
    }

    public Documento buscarDocumentoDoProcesso(Long processoId, Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento nao encontrado"));

        if (documento.getProcesso() == null || !documento.getProcesso().getId().equals(processoId)) {
            throw new RuntimeException("Documento nao pertence ao processo informado");
        }

        return documento;
    }

    public Pagamento buscarUltimoPagamento(Long processoId) {
        return pagamentoRepository.findTopByProcessoIdOrderByIdDesc(processoId);
    }

    public RegistroEntrega buscarUltimoRegistroEntrega(Long processoId) {
        return registroEntregaRepository.findTopByProcessoIdOrderByIdDesc(processoId);
    }

    public ValidacaoRecebimento buscarUltimaValidacaoRecebimento(Long processoId) {
        return validacaoRecebimentoRepository.findTopByRegistroEntregaProcessoIdOrderByIdDesc(processoId);
    }

    public List<HistoricoProcesso> listarHistorico(Long processoId) {
        return historicoProcessoRepository.findByProcessoIdOrderByDataTransicaoAsc(processoId);
    }

    @Transactional
    public Processo analisarCompra(Long processoId,
                                   Usuario usuario,
                                   TipoAquisicao tipoAquisicao,
                                   String fornecedorNome,
                                   String fornecedorCnpj,
                                   Double valorNegociado,
                                   Long fiscalId) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissao para analisar a compra");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("A analise so pode ocorrer quando o processo estiver aguardando GERAF");
        }

        if (tipoAquisicao == null) {
            throw new RuntimeException("Informe o tipo de aquisicao");
        }
        if (!StringUtils.hasText(fornecedorNome)) {
            throw new RuntimeException("Informe o fornecedor selecionado");
        }
        if (!StringUtils.hasText(fornecedorCnpj)) {
            throw new RuntimeException("Informe o CNPJ do fornecedor");
        }
        if (valorNegociado == null || valorNegociado <= 0) {
            throw new RuntimeException("Informe o valor negociado");
        }

        processo.setTipoAquisicao(tipoAquisicao);
        processo.setFornecedorNome(fornecedorNome.trim());
        processo.setFornecedorCnpj(fornecedorCnpj.trim());
        processo.setValorNegociado(valorNegociado);

        if (tipoAquisicao == TipoAquisicao.CONTRATO) {
            if (fiscalId == null) {
                throw new RuntimeException("Processos do tipo contrato exigem fiscal definido");
            }
            processo.setFiscal(buscarUsuario(fiscalId));
        } else {
            processo.setFiscal(null);
        }

        return processoRepository.save(processo);
    }

    @Transactional
    public void autorizarCompra(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("Estado invalido");
        }

        validarAnaliseAntesDaAutorizacao(processo);
        atualizarEstado(processo, usuario, StatusProcesso.AUTORIZACAO_EMITIDA, "Compra autorizada pelo GERAF.");
    }

    @Transactional
    public void rejeitarProcesso(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("Estado invalido");
        }

        atualizarEstado(processo, usuario, StatusProcesso.REJEITADO, "Processo rejeitado pelo GERAF.");
    }

    @Transactional
    public Pagamento solicitarPagamento(Long processoId,
                                        Usuario usuario,
                                        TipoPagamento tipoPagamento,
                                        Double valorSolicitado,
                                        LocalDate dataSolicitacao) {
        Processo processo = buscarPorId(processoId);

        if (!podeSolicitarPagamento(processo, usuario)) {
            throw new RuntimeException("Sem permissao para solicitar pagamento");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AUTORIZACAO_EMITIDA) {
            throw new RuntimeException("O processo precisa estar autorizado antes da solicitacao de pagamento");
        }

        if (tipoPagamento == null || valorSolicitado == null || valorSolicitado <= 0 || dataSolicitacao == null) {
            throw new RuntimeException("Preencha tipo, valor e data da solicitacao");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setProcesso(processo);
        pagamento.setSolicitadoPor(usuario);
        pagamento.setTipoPagamento(tipoPagamento);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValorSolicitado(valorSolicitado);
        pagamento.setDataSolicitacao(dataSolicitacao);

        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);
        atualizarEstado(processo, usuario, StatusProcesso.AGUARDANDO_PAGAMENTO, "Pagamento solicitado.");
        return pagamentoSalvo;
    }

    @Transactional
    public void aprovarPagamento(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);
        Pagamento pagamento = exigirPagamento(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GESTOR) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_PAGAMENTO || pagamento.getStatus() != StatusPagamento.PENDENTE) {
            throw new RuntimeException("Pagamento nao esta aguardando aprovacao");
        }

        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento.setAutorizadoPor(usuario);
        pagamentoRepository.save(pagamento);
        registrarHistorico(processo, usuario, processo.getEstadoAtual(), processo.getEstadoAtual(), "Pagamento aprovado pelo gestor.");
    }

    @Transactional
    public void registrarPagamento(Long processoId,
                                   Usuario usuario,
                                   Double valorPago,
                                   LocalDate dataPagamento,
                                   MultipartFile comprovanteFile) {
        Processo processo = buscarPorId(processoId);
        Pagamento pagamento = exigirPagamento(processoId);

        if (!podeRegistrarPagamento(usuario)) {
            throw new RuntimeException("Sem permissao para registrar pagamento");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_PAGAMENTO || pagamento.getStatus() != StatusPagamento.APROVADO) {
            throw new RuntimeException("Pagamento precisa estar aprovado antes do registro");
        }

        if (valorPago == null || valorPago <= 0 || dataPagamento == null) {
            throw new RuntimeException("Informe valor pago e data do pagamento");
        }

        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setValorPago(valorPago);
        pagamento.setDataPagamento(dataPagamento);
        pagamentoRepository.save(pagamento);

        salvarDocumento(processo, usuario, comprovanteFile, TipoDocumento.COMPROVANTE_PAGAMENTO);
        atualizarEstado(processo, usuario, StatusProcesso.PAGO, "Pagamento registrado.");
    }

    @Transactional
    public void registrarRecebimento(Long processoId,
                                     Usuario usuario,
                                     String nomeRecebedor,
                                     boolean conforme,
                                     String descricaoProblema,
                                     AcaoNaoConformidade acaoNaoConformidade,
                                     MultipartFile evidenciaFile) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.DEMANDANTE) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.PAGO) {
            throw new RuntimeException("O processo precisa estar pago antes do recebimento");
        }

        if (!StringUtils.hasText(nomeRecebedor)) {
            throw new RuntimeException("Informe o nome do recebedor");
        }

        RegistroEntrega entrega = new RegistroEntrega();
        entrega.setProcesso(processo);
        entrega.setRegistradoPor(usuario);
        entrega.setNomeRecebedor(nomeRecebedor.trim());
        entrega.setDataRegistro(LocalDateTime.now());
        RegistroEntrega entregaSalva = registroEntregaRepository.save(entrega);

        ValidacaoRecebimento validacao = new ValidacaoRecebimento();
        validacao.setRegistroEntrega(entregaSalva);
        validacao.setValidadoPor(usuario);
        validacao.setDataValidacao(LocalDateTime.now());
        validacao.setStatus(conforme ? StatusValidacaoRecebimento.CONFORME : StatusValidacaoRecebimento.NAO_CONFORME);

        if (!conforme) {
            if (!StringUtils.hasText(descricaoProblema) || acaoNaoConformidade == null) {
                throw new RuntimeException("Recebimentos nao conformes exigem descricao do problema e acao adotada");
            }
            validacao.setDescricaoProblema(descricaoProblema.trim());
            validacao.setAcaoNaoConformidade(acaoNaoConformidade);
        }

        validacaoRecebimentoRepository.save(validacao);
        salvarDocumento(processo, usuario, evidenciaFile, TipoDocumento.EVIDENCIA_RECEBIMENTO);

        atualizarEstado(
                processo,
                usuario,
                conforme ? StatusProcesso.RECEBIDO_CONFORME : StatusProcesso.RECEBIDO_NAO_CONFORME,
                conforme ? "Recebimento registrado como conforme." : "Recebimento registrado como nao conforme."
        );
    }

    @Transactional
    public void validarNotaFiscal(Long processoId,
                                  Usuario usuario,
                                  String numeroNotaFiscal,
                                  Double valorNotaFiscal,
                                  String justificativaDivergencia,
                                  MultipartFile notaFiscalFile) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GECONT) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.RECEBIDO_CONFORME
                && processo.getEstadoAtual() != StatusProcesso.RECEBIDO_NAO_CONFORME) {
            throw new RuntimeException("O processo precisa ter recebimento registrado antes da validacao da NF");
        }

        if (!StringUtils.hasText(numeroNotaFiscal) || valorNotaFiscal == null || valorNotaFiscal <= 0) {
            throw new RuntimeException("Informe numero e valor da nota fiscal");
        }

        Double referencia = processo.getValorNegociado() != null ? processo.getValorNegociado() : processo.getValorTotal();
        boolean divergencia = referencia != null && Math.abs(referencia - valorNotaFiscal) > DIVERGENCIA_TOLERANCIA;

        if (divergencia && !StringUtils.hasText(justificativaDivergencia)) {
            throw new RuntimeException("Divergencia relevante entre nota fiscal e valor negociado exige justificativa");
        }

        processo.setNumeroNotaFiscal(numeroNotaFiscal.trim());
        processo.setValorNotaFiscal(valorNotaFiscal);
        processo.setJustificativaDivergenciaNf(StringUtils.hasText(justificativaDivergencia) ? justificativaDivergencia.trim() : null);
        processoRepository.save(processo);

        salvarDocumento(processo, usuario, notaFiscalFile, TipoDocumento.NOTA_FISCAL);
        atualizarEstado(processo, usuario, StatusProcesso.NOTA_FISCAL_VALIDADA, "Nota fiscal validada pelo GECONT.");
        atualizarEstado(processo, usuario, StatusProcesso.ENCERRADO, "Processo encerrado com resumo final disponivel.");
        processo.setDataEncerramento(LocalDateTime.now());
        processoRepository.save(processo);
    }

    public String gerarResumoProcesso(Processo processo) {
        Pagamento pagamento = buscarUltimoPagamento(processo.getId());
        ValidacaoRecebimento validacao = buscarUltimaValidacaoRecebimento(processo.getId());

        return String.join(" | ",
                "Processo: " + processo.getIdentificador(),
                "Demandante: " + (processo.getSetorDemandante() != null ? processo.getSetorDemandante().getNome() : "Nao informado"),
                "Tipo: " + valorOuPadrao(processo.getTipoAquisicao(), "Nao definido"),
                "Fornecedor: " + valorOuPadrao(processo.getFornecedorNome(), "Nao definido"),
                "Valor negociado: " + valorOuPadrao(processo.getValorNegociado(), "Nao informado"),
                "Pagamento: " + (pagamento != null ? pagamento.getStatus() : "Nao solicitado"),
                "Recebimento: " + (validacao != null ? validacao.getStatus() : "Nao registrado"),
                "Estado atual: " + processo.getEstadoAtual()
        );
    }

    private String gerarIdentificador() {
        return "PROC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean filtroBusca(Processo processo, String busca) {
        if (busca == null || busca.isBlank()) {
            return true;
        }

        String termo = busca.trim().toLowerCase();
        return processo.getTitulo().toLowerCase().contains(termo)
                || processo.getIdentificador().toLowerCase().contains(termo)
                || (processo.getSetorDemandante() != null
                && processo.getSetorDemandante().getNome() != null
                && processo.getSetorDemandante().getNome().toLowerCase().contains(termo));
    }

    private boolean filtroSetor(Usuario usuario, Processo processo, Long setorId) {
        if (usuario.getPerfil() == PerfilUsuario.DEMANDANTE || setorId == null) {
            return true;
        }

        return processo.getSetorDemandante() != null
                && processo.getSetorDemandante().getSetor() != null
                && setorId.equals(processo.getSetorDemandante().getSetor().getId());
    }

    private void validarAnaliseAntesDaAutorizacao(Processo processo) {
        if (processo.getTipoAquisicao() == null) {
            throw new RuntimeException("Defina o tipo de aquisicao antes de autorizar");
        }
        if (!StringUtils.hasText(processo.getFornecedorNome())
                || !StringUtils.hasText(processo.getFornecedorCnpj())
                || processo.getValorNegociado() == null) {
            throw new RuntimeException("Preencha fornecedor, CNPJ e valor negociado antes de autorizar");
        }
        if (processo.getTipoAquisicao() == TipoAquisicao.CONTRATO && processo.getFiscal() == null) {
            throw new RuntimeException("Contratos exigem fiscal definido antes da autorizacao");
        }
    }

    private boolean podeSolicitarPagamento(Processo processo, Usuario usuario) {
        if (usuario.getPerfil() == PerfilUsuario.GERAF) {
            return true;
        }
        return processo.getTipoAquisicao() == TipoAquisicao.CONTRATO
                && processo.getFiscal() != null
                && processo.getFiscal().getId().equals(usuario.getId());
    }

    private boolean podeRegistrarPagamento(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.GERAF || usuario.getPerfil() == PerfilUsuario.ADMIN;
    }

    private Pagamento exigirPagamento(Long processoId) {
        Pagamento pagamento = buscarUltimoPagamento(processoId);
        if (pagamento == null) {
            throw new RuntimeException("Nenhum pagamento encontrado para o processo");
        }
        return pagamento;
    }

    private void atualizarEstado(Processo processo, Usuario usuario, StatusProcesso novoEstado, String observacao) {
        StatusProcesso estadoAnterior = processo.getEstadoAtual();
        processo.setEstadoAtual(novoEstado);
        processoRepository.save(processo);
        registrarHistorico(processo, usuario, estadoAnterior, novoEstado, observacao);
    }

    private void registrarHistorico(Processo processo,
                                    Usuario usuario,
                                    StatusProcesso estadoAnterior,
                                    StatusProcesso estadoNovo,
                                    String observacao) {
        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setProcesso(processo);
        historico.setUsuario(usuario);
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(estadoNovo);
        historico.setObservacao(observacao);
        historico.setDataTransicao(LocalDateTime.now());
        historicoProcessoRepository.save(historico);
    }

    private String valorOuPadrao(Object valor, String padrao) {
        return valor != null ? String.valueOf(valor) : padrao;
    }

    private void salvarDocumento(Processo processo, Usuario usuario, MultipartFile arquivo, TipoDocumento tipoDocumento) {
        if (arquivo == null || arquivo.isEmpty()) {
            return;
        }

        try {
            Files.createDirectories(uploadDir);

            String nomeOriginal = StringUtils.cleanPath(arquivo.getOriginalFilename());
            String extensao = "";
            int ultimoPonto = nomeOriginal.lastIndexOf('.');
            if (ultimoPonto >= 0) {
                extensao = nomeOriginal.substring(ultimoPonto);
            }

            String nomeSalvo = processo.getIdentificador()
                    + "-"
                    + tipoDocumento.name().toLowerCase()
                    + "-"
                    + UUID.randomUUID()
                    + extensao;

            Path destino = uploadDir.resolve(nomeSalvo).normalize();
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            Documento documento = new Documento();
            documento.setTipo(tipoDocumento);
            documento.setNomeArquivo(nomeOriginal);
            documento.setCaminhoArquivo(destino.toString());
            documento.setDataUpload(LocalDateTime.now());
            documento.setProcesso(processo);
            documento.setUsuarioUpload(usuario);

            documentoRepository.save(documento);
            logger.info("Documento salvo para processo {} em {}", processo.getIdentificador(), destino);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + arquivo.getOriginalFilename(), e);
        }
    }
}
