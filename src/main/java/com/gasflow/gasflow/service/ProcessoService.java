package com.gasflow.gasflow.service;

import com.gasflow.gasflow.enums.PerfilUsuario;
import com.gasflow.gasflow.enums.StatusProcesso;
import com.gasflow.gasflow.enums.TipoDocumento;
import com.gasflow.gasflow.model.Documento;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import com.gasflow.gasflow.repository.DocumentoRepository;
import com.gasflow.gasflow.repository.ProcessoRepository;
import com.gasflow.gasflow.repository.UsuarioRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcessoService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoService.class);

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentoRepository;
    private final Path uploadDir;

    public ProcessoService(ProcessoRepository processoRepository,
                           UsuarioRepository usuarioRepository,
                           DocumentoRepository documentoRepository,
                           @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentoRepository = documentoRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // =========================
    // CRIACAO
    // =========================
    @Transactional
    public Processo criarProcesso(Processo processo,
                                  Long usuarioId,
                                  MultipartFile pabsFile,
                                  MultipartFile memorialFile,
                                  MultipartFile mapaCotacaoFile,
                                  MultipartFile propostasFile,
                                  MultipartFile outrosFile) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        if (pabsFile == null || pabsFile.isEmpty()) {
            throw new RuntimeException("O arquivo PABS e obrigatorio");
        }

        logger.info("Criando processo com upload. PABS recebido: {}, memorial: {}, mapa: {}, proposta: {}, outros: {}",
                !pabsFile.isEmpty(),
                memorialFile != null && !memorialFile.isEmpty(),
                mapaCotacaoFile != null && !mapaCotacaoFile.isEmpty(),
                propostasFile != null && !propostasFile.isEmpty(),
                outrosFile != null && !outrosFile.isEmpty());

        processo.setIdentificador(gerarIdentificador());
        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_ANALISE_GERAF);
        processo.setSetorDemandante(usuario);

        Processo processoSalvo = processoRepository.save(processo);

        salvarDocumento(processoSalvo, usuario, pabsFile, TipoDocumento.PABS);
        salvarDocumento(processoSalvo, usuario, memorialFile, TipoDocumento.MEMORIAL_DESCRITIVO);
        salvarDocumento(processoSalvo, usuario, mapaCotacaoFile, TipoDocumento.MAPA_COTACAO);
        salvarDocumento(processoSalvo, usuario, propostasFile, TipoDocumento.PROPOSTA);
        salvarDocumento(processoSalvo, usuario, outrosFile, TipoDocumento.OUTRO);

        return processoSalvo;
    }

    // =========================
    // LISTAGEM
    // =========================
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
                                           com.gasflow.gasflow.enums.TipoProcesso tipoProcesso,
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

    // =========================
    // FLUXO DO PROCESSO
    // =========================

    // GERAF
    public void autorizarCompra(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("Estado invalido");
        }

        processo.setEstadoAtual(StatusProcesso.AUTORIZACAO_EMITIDA);
        processoRepository.save(processo);
    }

    public void rejeitarProcesso(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GERAF) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_ANALISE_GERAF) {
            throw new RuntimeException("Estado invalido");
        }

        processo.setEstadoAtual(StatusProcesso.REJEITADO);
        processoRepository.save(processo);
    }

    // GESTOR
    public void aprovarPagamento(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.GESTOR) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.AUTORIZACAO_EMITIDA) {
            throw new RuntimeException("Estado invalido");
        }

        processo.setEstadoAtual(StatusProcesso.AGUARDANDO_PAGAMENTO);
        processoRepository.save(processo);
    }

    // FINANCEIRO / GERAF
    public void registrarPagamento(Long processoId, Usuario usuario) {
        Processo processo = buscarPorId(processoId);

        if (processo.getEstadoAtual() != StatusProcesso.AGUARDANDO_PAGAMENTO) {
            throw new RuntimeException("Estado invalido");
        }

        processo.setEstadoAtual(StatusProcesso.PAGO);
        processoRepository.save(processo);
    }

    // DEMANDANTE
    public void registrarRecebimento(Long processoId, Usuario usuario, boolean conforme) {
        Processo processo = buscarPorId(processoId);

        if (usuario.getPerfil() != PerfilUsuario.DEMANDANTE) {
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.PAGO) {
            throw new RuntimeException("Estado invalido");
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
            throw new RuntimeException("Sem permissao");
        }

        if (processo.getEstadoAtual() != StatusProcesso.RECEBIDO_CONFORME) {
            throw new RuntimeException("Estado invalido");
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
