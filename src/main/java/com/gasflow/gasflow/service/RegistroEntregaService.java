package com.gasflow.gasflow.service;

import com.gasflow.gasflow.model.RegistroEntrega;
import com.gasflow.gasflow.repository.RegistroEntregaRepository;
import org.springframework.stereotype.Service;

@Service
public class RegistroEntregaService {

    private final RegistroEntregaRepository registroEntregaRepository;

    public RegistroEntregaService(RegistroEntregaRepository registroEntregaRepository) {
        this.registroEntregaRepository = registroEntregaRepository;
    }

    public RegistroEntrega buscarUltimoPorProcesso(Long processoId) {
        return registroEntregaRepository
                .findTopByProcessoIdOrderByIdDesc(processoId)
                .orElse(null);
    }

    public RegistroEntrega salvar(RegistroEntrega registro) {
        return registroEntregaRepository.save(registro);
    }
}