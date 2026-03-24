package com.gasflow.gasflow.service;

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

    public Processo criarProcesso(Processo processo, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        processo.setIdentificador(gerarIdentificador());
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE_GERAF);
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
}