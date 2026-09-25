package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.AuditoriaResponseDTO;
import co.sena.adso.porteria.entity.Auditoria;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.repository.AuditoriaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listar(int pagina) {
        return auditoriaRepository.findAllByOrderByFechaDescIdDesc(PageRequest.of(pagina, 50)).map(AuditoriaResponseDTO::fromEntity);
    }

    /** Acciones que hace el propio sistema, sin una persona detrás (por ejemplo el cierre de medianoche). */
    public void registrarSistema(String tabla, String accion, String detalles, LocalDateTime fecha) {
        auditoriaRepository.save(new Auditoria(null, "SISTEMA", tabla, 0L, accion, null, null, detalles, fecha));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
