package com.gasflow.gasflow.controller;

import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.service.DocumentoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> visualizarDocumento(@PathVariable Long id) {

        try {
            Documento doc = documentoService.buscarPorId(id);

            Path path = Paths.get(doc.getCaminhoArquivo().replace("\\", "/"));
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);

            if (contentType == null) {
                contentType = doc.getNomeArquivo().endsWith(".pdf")
                        ? "application/pdf"
                        : "image/jpeg";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getNomeArquivo() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}