package co.sena.adso.porteria.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final JavaMailSender enviador;
    private final String host;
    private final String remitente;

    public CorreoService(JavaMailSender enviador, @Value("${spring.mail.host:}") String host,
                         @Value("${app.correo.remitente:}") String remitente) {
        this.enviador = enviador;
        this.host = host;
        this.remitente = remitente;
    }

    public boolean configurado() {
        return !host.isBlank();
    }

    // Devuelve false en vez de lanzar: un correo que no sale no debe tumbar la operación
    public boolean enviar(String para, String asunto, String html) {
        if (!configurado()) {
            log.warn("Correo sin enviar: falta SMTP_HOST");
            return false;
        }
        try {
            MimeMessage mensaje = enviador.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, "UTF-8");
            if (!remitente.isBlank()) {
                ayudante.setFrom(remitente);
            }
            ayudante.setTo(para);
            ayudante.setSubject(asunto);
            ayudante.setText(html, true);
            enviador.send(mensaje);
            return true;
        } catch (MessagingException | MailException e) {
            log.warn("No se pudo enviar un correo: {}", e.getMessage());
            return false;
        }
    }
}
