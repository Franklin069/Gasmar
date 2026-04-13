package com.gasflow.gasflow.service;

import com.gasflow.gasflow.dto.AcaoProcessoDTO;
import com.gasflow.gasflow.enums.*;
import com.gasflow.gasflow.model.*;
import com.gasflow.gasflow.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProcessoService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final HistoricoProcessoRepository historicoProcessoRepositoryRepository;
    private final PagamentoService pagamentoService;
    private final RegistroEntregaRepository registroEntregaRepository;
    private final EmailService emailService;

    @Autowired
    private FileStorageService fileStorageService;

    public ProcessoService(ProcessoRepository processoRepository, UsuarioRepository usuarioRepository, PagamentoRepository pagamentoRepository, HistoricoProcessoRepository historicoProcessoRepositoryRepository, PagamentoService pagamentoService, RegistroEntregaRepository registroEntregaRepository, EmailService emailService) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.historicoProcessoRepositoryRepository = historicoProcessoRepositoryRepository;
        this.pagamentoService = pagamentoService;
        this.registroEntregaRepository = registroEntregaRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Processo criarProcesso(Processo processo, Usuario demandante) {
        processo.setIdentificador(gerarIdentificador());

        processo.setSetorDemandante(demandante);
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE);

        return processoRepository.save(processo);
    }

    public List<Processo> listarTodos() {
        return processoRepository.findAllByOrderByIdDesc();
    }

    public List<Processo> listarRecentes(Usuario usuario) {
        if (usuario != null && usuario.getCargo() == Cargo.DEMANDANTE) {
            return processoRepository.findTop10BySetorDemandanteOrderByIdDesc(usuario);
        }
        return processoRepository.findTop10ByOrderByIdDesc();
    }

    public long contarPorEstado(Usuario usuario, StatusProcesso statusProcesso) {
        if (usuario != null && usuario.getCargo() == Cargo.DEMANDANTE) {
            return processoRepository.countByEstadoAtualAndSetorDemandante(statusProcesso, usuario);
        }
        return processoRepository.countByEstadoAtual(statusProcesso);
    }

    public Processo buscarPorId(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));
    }

    public synchronized String gerarIdentificador() {
        int anoAtual = Year.now().getValue();
        String sufixoAno = "-" + anoAtual;

        String ultimoIdentificador = processoRepository.findMaxIdentificadorByAno(sufixoAno);

        int proximoNumero = 1;

        if (ultimoIdentificador != null) {
            String numeroStr = ultimoIdentificador.split("-")[0];
            proximoNumero = Integer.parseInt(numeroStr) + 1;
        }

        return String.format("%03d-%d", proximoNumero, anoAtual);
    }

    public List<Processo> listarPorUsuario(Usuario usuario) {
        if (usuario != null && usuario.getCargo() == Cargo.DEMANDANTE) {
            return processoRepository.findBySetorDemandanteOrderByIdDesc(usuario);
        }
        return processoRepository.findAllByOrderByIdDesc();
    }

    public List<Processo> listarComFiltros(Usuario usuario,
                                           String busca,
                                           StatusProcesso status,
                                           com.gasflow.gasflow.enums.TipoProcesso tipoProcesso,
                                           Long setorId) {
        return listarPorUsuario(usuario).stream()
                .filter(processo -> filtroBusca(processo, busca))
                .filter(processo -> status == null || processo.getEstadoAtual() == status)
                .filter(processo -> tipoProcesso == null || processo.getTipoProcesso() == tipoProcesso)
                .filter(processo -> filtroSetor(processo, usuario, setorId))
                .collect(Collectors.toList());
    }

    private boolean filtroBusca(Processo processo, String busca) {
        if (busca == null || busca.isBlank()) {
            return true;
        }

        String termo = busca.trim().toLowerCase();
        String titulo = processo.getTitulo() != null ? processo.getTitulo().toLowerCase() : "";
        String identificador = processo.getIdentificador() != null ? processo.getIdentificador().toLowerCase() : "";

        return titulo.contains(termo)
                || identificador.contains(termo)
                || (processo.getSetorDemandante() != null
                && processo.getSetorDemandante().getNome() != null
                && processo.getSetorDemandante().getNome().toLowerCase().contains(termo));
    }

    private boolean filtroSetor(Processo processo, Usuario usuario, Long setorId) {
        if (usuario != null && usuario.getCargo() == Cargo.DEMANDANTE) {
            return true;
        }

        if (setorId == null) {
            return true;
        }

        return processo.getSetorDemandante() != null
                && processo.getSetorDemandante().getSetor() != null
                && setorId.equals(processo.getSetorDemandante().getSetor().getId());
    }

    public List<AcaoProcessoDTO> resolverAcoes(Processo processo, Usuario usuario) {

        List<AcaoProcessoDTO> acoes = new ArrayList<>();

        if (processo == null || usuario == null || processo.getEstadoAtual() == null) {
            return acoes;
        }

        String setorUsuario = obterSiglaSetor(usuario);
        Cargo cargo = usuario.getCargo();

        if (processo.getStatusGecont() == StatusGecont.AGUARDANDO_VALIDACAO
                && processo.getEstadoAtual() != StatusProcesso.NF_NAO_CONFORME
                && processo.getEstadoAtual() != StatusProcesso.SOLICITACAO_NAO_CONFORME
                && processo.getEstadoAtual() != StatusProcesso.RECEBIDO_NAO_CONFORME
                && "GECONT".equalsIgnoreCase(setorUsuario)) {

            acoes.add(new AcaoProcessoDTO("VALIDAR_NOTA_FISCAL"));
        }

        switch (processo.getEstadoAtual()) {

            case AGUARDANDO_ANALISE:

                if ("GERAF".equalsIgnoreCase(setorUsuario) && processo.getFiscal() == null) {
                    acoes.add(new AcaoProcessoDTO("ANALISE_PROCESSO"));
                }

                if (processo.getFiscal() != null &&
                        processo.getFiscal().getId().equals(usuario.getId())) {

                    acoes.add(new AcaoProcessoDTO("ANALISE_FISCAL"));
                }

                break;

            case AGUARDANDO_RECEBIMENTO_EXECUCAO:
                if (processo.getFiscal() != null &&
                        processo.getFiscal().getId().equals(usuario.getId())) {

                    acoes.add(new AcaoProcessoDTO("CONFIRMAR_RECEBIMENTO"));

                } else if (processo.getFiscal() == null &&
                        "GERAF".equalsIgnoreCase(setorUsuario)) {

                    acoes.add(new AcaoProcessoDTO("CONFIRMAR_RECEBIMENTO"));
                }
                break;

            case AGUARDANDO_VALIDACAO_MATERIAL:
                if (mesmoSetorDemandante(processo, usuario)
                        && processo.getTipoProcesso() == TipoProcesso.COMPRA_BEM) {

                    acoes.add(new AcaoProcessoDTO("VALIDAR_MATERIAL"));
                }
                break;

            case AGUARDANDO_SOLICITACAO_PAGAMENTO:
                if (processo.getFiscal() != null &&
                        processo.getFiscal().getId().equals(usuario.getId())) {

                    acoes.add(new AcaoProcessoDTO("SOLICITAR_PAGAMENTO"));

                } else if (processo.getFiscal() == null &&
                        "GERAF".equalsIgnoreCase(setorUsuario)) {

                    acoes.add(new AcaoProcessoDTO("SOLICITAR_PAGAMENTO"));
                }
                break;

            case AGUARDANDO_AUTORIZACAO_PAGAMENTO:
                if (cargo == Cargo.GESTOR &&
                        mesmoSetorDemandante(processo, usuario)) {

                    acoes.add(new AcaoProcessoDTO("APROVAR_PAGAMENTO"));
                }
                break;

            case AGUARDANDO_EFETUACAO_PAGAMENTO:
                if ("GERAF".equalsIgnoreCase(setorUsuario)) {
                    acoes.add(new AcaoProcessoDTO("EXECUTAR_PAGAMENTO"));
                }
                break;
        }

        return acoes;
    }

    @Transactional
    public Processo conduzirProcesso(
            Long processoId, Double valorTotal, TipoAquisicao tipoAquisicao,
            TipoProcesso tipoProcesso, Long fiscalId, boolean solicitarAdiantamento, Usuario usuarioLogado
    ) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        processo.setValorTotal(valorTotal);
        processo.setTipoAquisicao(tipoAquisicao);
        processo.setTipoProcesso(tipoProcesso);

        if (fiscalId != null) {
            Usuario fiscal = usuarioRepository.findById(fiscalId)
                    .orElseThrow(() -> new RuntimeException("Fiscal não encontrado"));

            processo.setFiscal(fiscal);
            processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE);
        } else {
            processo.setFiscal(null);

            if (solicitarAdiantamento) {
                processo.setEstadoAtual(StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO);
            } else {
                processo.setEstadoAtual(StatusProcesso.AGUARDANDO_RECEBIMENTO_EXECUCAO);
            }
        }

        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(processo.getEstadoAtual());
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Compra realizada pela GERAF");
        historico.setProcesso(processo);
        historico.setUsuario(usuarioLogado);

        historicoProcessoRepositoryRepository.save(historico);
        
        notificarFiscalSeNecessario(processo, "Você foi designado como fiscal do processo.");

        return processo;
    }


    @Transactional
    public Processo analiseFiscal(Long processoId, boolean solicitarAdiantamento, Usuario usuarioLogado) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        if (solicitarAdiantamento) {
            processo.setEstadoAtual(StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO);
        } else {
            processo.setEstadoAtual(StatusProcesso.AGUARDANDO_RECEBIMENTO_EXECUCAO);
        }

        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(processo.getEstadoAtual());
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Análise do fiscal concluída");
        historico.setProcesso(processo);
        historico.setUsuario(usuarioLogado);

        historicoProcessoRepositoryRepository.save(historico);
        notificarMudancaPrincipal(processo, "Análise fiscal concluída.");

        return processo;
    }

    @Transactional
    public void solicitarPagamento(
            Long processoId,
            Double valor,
            boolean adiantamento,
            Usuario usuario
    ) {

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        Pagamento pagamento = new Pagamento();
        pagamento.setProcesso(processo);
        pagamento.setSolicitadoPor(usuario);
        pagamento.setValorSolicitado(valor);
        pagamento.setStatus(StatusPagamento.SOLICITADO);
        pagamento.setDataSolicitacao(java.time.LocalDate.now());

        if (adiantamento) {
            pagamento.setTipoPagamento(TipoPagamento.ADIANTAMENTO);
        } else {
            pagamento.setTipoPagamento(TipoPagamento.PARCIAL);
        }

        pagamentoRepository.save(pagamento);

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_AUTORIZACAO_PAGAMENTO);
        processo.setStatusGecont(StatusGecont.AGUARDANDO_VALIDACAO);
        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(StatusProcesso.AGUARDANDO_AUTORIZACAO_PAGAMENTO);
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Pagamento solicitado");
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoProcessoRepositoryRepository.save(historico);
        notificarPagamento(processo, "Solicitação de pagamento registrada.");
    }

    @Transactional
    public void aprovarPagamento(Long processoId, Long pagamentoId, Usuario usuario) {

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_EFETUACAO_PAGAMENTO);

        pagamento.setStatus(StatusPagamento.AUTORIZADO);
        pagamento.setAutorizadoPor(usuario);

        processoRepository.save(processo);
        pagamentoRepository.save(pagamento);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(StatusProcesso.AGUARDANDO_EFETUACAO_PAGAMENTO);
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Pagamento aprovado pelo gestor");
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoProcessoRepositoryRepository.save(historico);
        notificarPagamento(processo, "Pagamento autorizado pelo gestor.");
    }

    @Transactional
    public void executarPagamento(
            Processo processo,
            Pagamento pagamento,
            Double valorPago,
            Usuario usuario
    ) {

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setValorPago(valorPago);
        pagamento.setDataPagamento(LocalDate.now());

        pagamentoRepository.save(pagamento);

        Double valorTotal = processo.getValorTotal() != null ? processo.getValorTotal() : 0.0;
        Double valorPagoTotal = pagamentoService.calcularValorPago(processo.getId());

        Double restante = valorTotal - (valorPagoTotal != null ? valorPagoTotal : 0);

        StatusProcesso novoEstado;

        if (pagamento.getTipoPagamento() == TipoPagamento.ADIANTAMENTO) {
            novoEstado = StatusProcesso.AGUARDANDO_RECEBIMENTO_EXECUCAO;

        } else {

            if (restante <= 0) {
                novoEstado = StatusProcesso.ENCERRADO;

            } else {
                novoEstado = StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO;
            }
        }

        processo.setEstadoAtual(novoEstado);
        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(novoEstado);
        historico.setDataTransicao(LocalDateTime.now());
        String observacao;
        if (novoEstado == StatusProcesso.ENCERRADO) {
            observacao = "Processo encerrado após pagamento total";
        } else if (pagamento.getTipoPagamento() == TipoPagamento.ADIANTAMENTO) {
            observacao = "Pagamento de adiantamento realizado pela GERAF";
        } else {
            observacao = "Pagamento executado pela GERAF";
        }
        historico.setObservacao(observacao);
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoProcessoRepositoryRepository.save(historico);
        notificarPagamento(processo, observacao);
    }

    @Transactional
    public void confirmarExecucao(Long processoId, Usuario usuario) {

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        RegistroEntrega registro = new RegistroEntrega();
        registro.setProcesso(processo);
        registro.setRegistradoPor(usuario);
        registro.setDataRegistro(LocalDateTime.now());

        registroEntregaRepository.save(registro);

        StatusProcesso novoEstado;

        if (processo.getTipoProcesso() == TipoProcesso.COMPRA_BEM) {
            novoEstado = StatusProcesso.AGUARDANDO_VALIDACAO_MATERIAL;
        } else {
            novoEstado = StatusProcesso.AGUARDANDO_SOLICITACAO_PAGAMENTO;
        }

        processo.setEstadoAtual(novoEstado);
        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(novoEstado);
        historico.setDataTransicao(LocalDateTime.now());
        if (processo.getTipoProcesso() == TipoProcesso.COMPRA_BEM) {
            historico.setObservacao("Recebimento do material confirmado");
        } else {
            historico.setObservacao("Execução do serviço confirmada");
        }
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoProcessoRepositoryRepository.save(historico);
        notificarMudancaPrincipal(processo, historico.getObservacao());
    }

    @Transactional
    public void validarNotaFiscalGecont(
            Processo processo,
            Usuario usuario,
            String observacao
    ) {

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        processo.setStatusGecont(StatusGecont.VALIDADO);

        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setProcesso(processo);
        historico.setUsuario(usuario);
        historico.setDataTransicao(LocalDateTime.now());

        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(processo.getEstadoAtual());

        historico.setObservacao(
                observacao != null && !observacao.isBlank()
                        ? observacao
                        : "Nota fiscal validada pela GECONT"
        );

        historicoProcessoRepositoryRepository.save(historico);
        notificarMudancaPrincipal(processo, historico.getObservacao());
    }

    public boolean registrarInconformidade(Processo processo, Usuario usuario, String motivo) {

        if (processo == null || usuario == null || motivo == null || motivo.isBlank()) {
            return false;
        }

        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        if (estadoAnterior == StatusProcesso.RECEBIDO_NAO_CONFORME
                || estadoAnterior == StatusProcesso.SOLICITACAO_NAO_CONFORME
                || estadoAnterior == StatusProcesso.NF_NAO_CONFORME
                || processo.getStatusGecont() == StatusGecont.NAO_CONFORME) {
            return false;
        }

        StatusProcesso novoEstado;

        if (processo.getStatusGecont() == StatusGecont.AGUARDANDO_VALIDACAO &&
                "GECONT".equalsIgnoreCase(usuario.getSetor().getSigla())) {

            novoEstado = StatusProcesso.NF_NAO_CONFORME;
            processo.setStatusGecont(StatusGecont.NAO_CONFORME);

        } else {
            switch (estadoAnterior) {
                case AGUARDANDO_VALIDACAO_MATERIAL:
                    novoEstado = StatusProcesso.RECEBIDO_NAO_CONFORME;
                    break;

                case AGUARDANDO_AUTORIZACAO_PAGAMENTO:
                case AGUARDANDO_ANALISE:
                    novoEstado = StatusProcesso.SOLICITACAO_NAO_CONFORME;
                    break;

                default:
                    novoEstado = StatusProcesso.SOLICITACAO_NAO_CONFORME; 
            }
        }

        processo.setEstadoAtual(novoEstado);
        if (processo.getStatusGecont() == StatusGecont.AGUARDANDO_VALIDACAO
                || novoEstado == StatusProcesso.NF_NAO_CONFORME) {
            processo.setStatusGecont(StatusGecont.NAO_CONFORME);
        }
        processoRepository.save(processo);

        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(novoEstado);
        historico.setProcesso(processo);
        historico.setUsuario(usuario);
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Não conformidade registrada");
        historico.setObservacaoInconforme(motivo.trim());

        historicoProcessoRepositoryRepository.save(historico);
        notificarMudancaPrincipal(processo, "Não conformidade registrada no processo.");
        return true;
    }

    private void notificarFiscalSeNecessario(Processo processo, String mudanca) {
        if (processo.getFiscal() == null || processo.getFiscal().getEmail() == null) {
            return;
        }

        emailService.notificarMudancaProcesso(
                List.of(processo.getFiscal()),
                processo,
                "Fiscal designado",
                "Voce recebeu um novo processo",
                mudanca,
                obterDescricaoEstado(processo)
        );
    }

    private void notificarPagamento(Processo processo, String mudanca) {
        Set<Usuario> destinatarios = new LinkedHashSet<>();

        if (processo.getSetorDemandante() != null && processo.getSetorDemandante().getEmail() != null) {
            destinatarios.add(processo.getSetorDemandante());
        }

        if (processo.getFiscal() != null && processo.getFiscal().getEmail() != null) {
            destinatarios.add(processo.getFiscal());
        }

        if (processo.getSetorDemandante() != null && processo.getSetorDemandante().getSetor() != null) {
            usuarioRepository.findByCargoAndSetor(Cargo.GESTOR, processo.getSetorDemandante().getSetor())
                    .stream()
                    .forEach(destinatarios::add);
        }

        emailService.notificarMudancaProcesso(
                destinatarios,
                processo,
                "Pagamento",
                "Atualizacao no fluxo de pagamento",
                mudanca,
                obterDescricaoEstado(processo)
        );
    }

    private void notificarMudancaPrincipal(Processo processo, String mudanca) {
        Set<Usuario> destinatarios = new LinkedHashSet<>();

        if (processo.getSetorDemandante() != null && processo.getSetorDemandante().getEmail() != null) {
            destinatarios.add(processo.getSetorDemandante());
        }

        if (processo.getFiscal() != null && processo.getFiscal().getEmail() != null) {
            destinatarios.add(processo.getFiscal());
        }

        emailService.notificarMudancaProcesso(
                destinatarios,
                processo,
                "Processo",
                "Mudanca importante no processo",
                mudanca,
                obterDescricaoEstado(processo)
        );
    }

    private String obterSiglaSetor(Usuario usuario) {
        if (usuario == null || usuario.getSetor() == null || usuario.getSetor().getSigla() == null) {
            return "";
        }
        return usuario.getSetor().getSigla();
    }

    private boolean mesmoSetorDemandante(Processo processo, Usuario usuario) {
        return processo != null
                && processo.getSetorDemandante() != null
                && processo.getSetorDemandante().getSetor() != null
                && usuario != null
                && usuario.getSetor() != null
                && processo.getSetorDemandante().getSetor().getId() != null
                && processo.getSetorDemandante().getSetor().getId().equals(usuario.getSetor().getId());
    }

    private String obterDescricaoEstado(Processo processo) {
        if (processo == null || processo.getEstadoAtual() == null) {
            return "Em atualizacao";
        }
        return processo.getEstadoAtual().getDescricao();
    }

}
