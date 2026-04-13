package com.gasflow.gasflow.service;

import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository repository;

    public List<Documento> listarPorProcesso(Long processoId) {
        return repository.findByProcessoId(processoId);
    }

    public Documento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));
    }
}