package com.gasflow.gasflow.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final Path root = Paths.get("uploads", "processos");

    public String salvar(MultipartFile file, String identificadorProcesso, String categoria, String... subcategorias)
            throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Falha ao armazenar arquivo vazio.");
        }

        Path destino = root.resolve(identificadorProcesso).resolve(categoria);
        if (subcategorias != null) {
            for (String subcategoria : subcategorias) {
                if (subcategoria != null && !subcategoria.isBlank()) {
                    destino = destino.resolve(subcategoria);
                }
            }
        }

        Files.createDirectories(destino);

        String nomeOriginal = file.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            nomeOriginal = "arquivo_sem_nome_" + System.currentTimeMillis();
        }

        Path arquivoDestino = destino.resolve(nomeOriginal);
        Files.copy(file.getInputStream(), arquivoDestino, StandardCopyOption.REPLACE_EXISTING);
        return arquivoDestino.toString();
    }
}
