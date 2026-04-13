package com.gasflow.gasflow.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.gasflow.gasflow.model.Processo;
import com.gasflow.gasflow.model.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);

    @Value("${gasflow.gmail.enabled:false}")
    private boolean gmailEnabled;

    @Value("${gasflow.gmail.application-name:GasFlow System}")
    private String applicationName;

    @Value("${gasflow.gmail.sender-email:gasflowteste@gmail.com}")
    private String senderEmail;

    @Value("${gasflow.gmail.credentials-path:credentials.json}")
    private String credentialsPath;

    @Value("${gasflow.gmail.tokens-directory:tokens}")
    private String tokensDirectory;

    @Value("${gasflow.app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final TemplateEngine templateEngine;
    private NetHttpTransport httpTransport;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void enviarEmail(String destinatario, String assunto, String corpo) throws MessagingException, IOException, GeneralSecurityException {
        enviarEmail(destinatario, assunto, corpo, false);
    }

    @Async
    public void notificarMudancaProcesso(Collection<Usuario> destinatarios,
                                         Processo processo,
                                         String subtitulo,
                                         String titulo,
                                         String mudanca,
                                         String estadoAtual) {
        if (!gmailEnabled || destinatarios == null || destinatarios.isEmpty()) {
            return;
        }

        String assunto = titulo + " - " + processo.getIdentificador();

        for (Usuario destinatario : destinatarios) {
            if (destinatario == null || !StringUtils.hasText(destinatario.getEmail())) {
                continue;
            }

            try {
                enviarEmail(destinatario.getEmail(), assunto, montarCorpoHtml(destinatario, processo, subtitulo, titulo, mudanca, estadoAtual), true);
            } catch (Exception e) {
                log.warn("Erro ao enviar email para {}", destinatario.getEmail(), e);
            }
        }
    }

    public void enviarEmail(String destinatario, String assunto, String corpo, boolean html) throws MessagingException, IOException, GeneralSecurityException {
        if (!gmailEnabled || !StringUtils.hasText(destinatario)) {
            return;
        }

        Gmail service = new Gmail.Builder(getHttpTransport(), JSON_FACTORY, getCredentials(getHttpTransport()))
                .setApplicationName(applicationName)
                .build();

        String remetente = StringUtils.hasText(senderEmail) ? senderEmail : destinatario;
        MimeMessage mimeMessage = createEmail(destinatario, remetente, assunto, corpo, html);
        Message message = createMessageWithEmail(mimeMessage);

        service.users().messages().send("me", message).execute();
    }

    private NetHttpTransport getHttpTransport() throws GeneralSecurityException, IOException {
        if (httpTransport == null) {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        }
        return httpTransport;
    }

    private Credential getCredentials(NetHttpTransport transport) throws IOException {
        InputStream in = resolveCredentialsStream();
        if (in == null) {
            throw new FileNotFoundException("Arquivo de credenciais do Gmail nao encontrado: " + credentialsPath);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectory)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private InputStream resolveCredentialsStream() throws FileNotFoundException {
        if (!StringUtils.hasText(credentialsPath)) {
            return null;
        }

        File credentialsFile = new File(credentialsPath);
        if (credentialsFile.exists()) {
            return new FileInputStream(credentialsFile);
        }

        return EmailService.class.getResourceAsStream(credentialsPath.startsWith("/") ? credentialsPath : "/" + credentialsPath);
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText, boolean html) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        if (html) {
            email.setContent(bodyText, "text/html; charset=UTF-8");
        } else {
            email.setText(bodyText);
        }
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private String montarCorpoHtml(Usuario destinatario,
                                   Processo processo,
                                   String subtitulo,
                                   String titulo,
                                   String mudanca,
                                   String estadoAtual) {
        Context context = new Context();
        context.setVariable("subtitulo", subtitulo);
        context.setVariable("titulo", titulo);
        context.setVariable("destinatario", destinatario);
        context.setVariable("mensagem", mudanca + " Estado atual: " + estadoAtual + ".");
        context.setVariable("processo", processo);
        context.setVariable("linkProcesso", baseUrl + "/processos/" + processo.getId());
        return templateEngine.process("emails/process-update", context);
    }
}
