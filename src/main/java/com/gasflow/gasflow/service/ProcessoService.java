package com.gasflow.gasflow.service;

import com.gasflow.gasflow.dto.AcaoProcessoDTO;
import com.gasflow.gasflow.enums.*;
import com.gasflow.gasflow.model.HistoricoProcesso;
import com.gasflow.gasflow.model.Pagamento;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProcessoService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final HistoricoProcessoRepository historicoProcessoRepositoryRepository;


    public ProcessoService(ProcessoRepository processoRepository, UsuarioRepository usuarioRepository, PagamentoRepository pagamentoRepository, HistoricoProcessoRepository historicoProcessoRepositoryRepository) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.historicoProcessoRepositoryRepository = historicoProcessoRepositoryRepository;
    }

    public Processo criarProcesso(Processo processo, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        processo.setIdentificador(gerarIdentificador());
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE);
        processo.setSetorDemandante(usuario);

        return processoRepository.save(processo);
    }

    public List<Processo> listarTodos() {
        return processoRepository.findAll();
    }

    public Processo buscarPorId(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));
    }

    private String gerarIdentificador() {
        return "PROC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public AcaoProcessoDTO resolverAcao(Processo processo, Usuario usuario) {

        String setorUsuario = usuario.getSetor().getSigla();
        Cargo cargo = usuario.getCargo();

        switch (processo.getEstadoAtual()) {

            case AGUARDANDO_ANALISE:
                if ("GERAF".equalsIgnoreCase(setorUsuario)) {
                    return new AcaoProcessoDTO("ANALISE_PROCESSO");
                }
                break;

            case AGUARDANDO_RECEBIMENTO_EXECUCAO:
                if (processo.getFiscal() != null &&
                        processo.getFiscal().getId().equals(usuario.getId())) {

                    return new AcaoProcessoDTO("CONFIRMAR_RECEBIMENTO");

                } else if (processo.getFiscal() == null &&
                        "GERAF".equalsIgnoreCase(setorUsuario)) {

                    return new AcaoProcessoDTO("CONFIRMAR_RECEBIMENTO");
                }
                break;

            case AGUARDANDO_VALIDACAO_MATERIAL:
                if (processo.getSetorDemandante().getId().equals(usuario.getId()) &&
                        processo.getTipoProcesso() == TipoProcesso.COMPRA_BEM) {

                    return new AcaoProcessoDTO("VALIDAR_MATERIAL");
                }
                break;

            case AGUARDANDO_SOLICITACAO_PAGAMENTO:
                if (processo.getFiscal() != null &&
                        processo.getFiscal().getId().equals(usuario.getId())) {

                    return new AcaoProcessoDTO("SOLICITAR_PAGAMENTO");

                } else if (processo.getFiscal() == null &&
                        "GERAF".equalsIgnoreCase(setorUsuario)) {

                    return new AcaoProcessoDTO("SOLICITAR_PAGAMENTO");
                }
                break;

            case AGUARDANDO_AUTORIZACAO_PAGAMENTO:
                if (cargo == Cargo.GESTOR &&
                        usuario.getSetor().getId().equals(
                                processo.getSetorDemandante().getSetor().getId())) {

                    return new AcaoProcessoDTO("APROVAR_PAGAMENTO");
                }
                break;

            case AGUARDANDO_EFETUACAO_PAGAMENTO:
                if ("GERAF".equalsIgnoreCase(setorUsuario)) {
                    return new AcaoProcessoDTO("EXECUTAR_PAGAMENTO");
                }
                break;
        }

        return null;
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

        // 🔹 Criar pagamento
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

        // 🔹 Atualizar estado do processo
        StatusProcesso estadoAnterior = processo.getEstadoAtual();

        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_AUTORIZACAO_PAGAMENTO);
        processoRepository.save(processo);

        // 🔹 Histórico
        HistoricoProcesso historico = new HistoricoProcesso();
        historico.setEstadoAnterior(estadoAnterior);
        historico.setEstadoNovo(StatusProcesso.AGUARDANDO_AUTORIZACAO_PAGAMENTO);
        historico.setDataTransicao(LocalDateTime.now());
        historico.setObservacao("Pagamento solicitado");
        historico.setProcesso(processo);
        historico.setUsuario(usuario);

        historicoProcessoRepositoryRepository.save(historico);
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
    }



}