package com.gasflow.gasflow.service;

import com.gasflow.gasflow.enums.PerfilUsuario;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.ProcessoRepository;
import com.gasflow.gasflow.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProcessoService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProcessoService(ProcessoRepository processoRepository, UsuarioRepository usuarioRepository) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =========================
    // CRIAÇÃO
    // =========================
    public Processo criarProcesso(Processo processo, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        processo.setIdentificador(gerarIdentificador());
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE_GERAF);
        processo.setSetorDemandante(usuario);

        return processoRepository.save(processo);
    }

    // =========================
    // LISTAGEM
    // =========================
    public List<Processo> listarTodos() {
        return processoRepository.findAll();
    }

    public List<Processo> listarPorUsuario(Usuario usuario) {
        if (usuario.getPerfil() == PerfilUsuario.DEMANDANTE) {
            return processoRepository.findBySetorDemandante(usuario);
        }
        return processoRepository.findAll();
    }

    public Processo buscarPorId(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));
    }

    // =========================
    // FLUXO DO PROCESSO
    // =========================

    // GERAF
    public void autorizarCompra(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissão");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("Estado inválido");
        }

        processo.setEstadoAtual(StatusProcesso.AUTORIZACAO_EMITIDA);
        processoRepository.save(processo);
    }

    // GESTOR
    public void aprovarPagamento(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GESTOR) {
            throw new RuntimeException("Sem permissão");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AUTORIZACAO_EMITIDA) {
            throw new RuntimeException("Estado inválido");
        }

        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_PAGAMENTO);
        processoRepository.save(processo);
    }

    // FINANCEIRO / GERAF
    public void registrarPagamento(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_PAGAMENTO) {
            throw new RuntimeException("Estado inválido");
        }

        processo.setEstadoAtual(StatusProcesso.PAGO);
        processoRepository.save(processo);
    }

    // DEMANDANTE
    public void registrarRecebimento(Long processoId, Usuario usuario, boolean conforme) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.DEMANDANTE) {
            throw new RuntimeException("Sem permissão");
        }

        if (processo.getEstadoAtual() != StatusProcesso.PAGO) {
            throw new RuntimeException("Estado inválido");
        }

        if (conforme) {
            processo.setEstadoAtual(StatusProcesso.RECEBIDO_CONFORME);
        } else {
            processo.setEstadoAtual(StatusProcesso.RECEBIDO_NAO_CONFORME);
        }

        processoRepository.save(processo);
    }

    // GECONT
    public void validarNotaFiscal(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GECONT) {
            throw new RuntimeException("Sem permissão");
        }

        if (processo.getEstadoAtual() != StatusProcesso.RECEBIDO_CONFORME) {
            throw new RuntimeException("Estado inválido");
        }

        processo.setEstadoAtual(StatusProcesso.NOTA_FISCAL_VALIDADA);
        processoRepository.save(processo);
    }

    // =========================
    // UTIL
    // =========================
    private String gerarIdentificador() {
        return "PROC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}