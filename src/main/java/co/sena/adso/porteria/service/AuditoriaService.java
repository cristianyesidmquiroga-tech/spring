package co.sena.adso.porteria.service;

import co.sena.adso.porteria.entity.Auditoria;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.AuditoriaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final Clock reloj;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, Clock reloj) {
        this.auditoriaRepository = auditoriaRepository;
        this.reloj = reloj;
    }

    public void registrar(Usuario autor, String tabla, Long registroId, String accion,
                          String autorizadoPor, String motivo, String detalles) {
        auditoriaRepository.save(new Auditoria(autor, tabla, registroId, accion,
                limpiar(autorizadoPor), limpiar(motivo), detalles, LocalDateTime.now(reloj)));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
