package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Mensaje;
import java.time.LocalDateTime;
import java.util.List;

// El asesor solo ve los últimos cuatro dígitos del documento y nunca el correo
public record ConversacionResponseDTO(Persona persona, List<Item> mensajes) {

    public record Persona(Long id, String nombre, String cargo, String documento, boolean tieneFoto, String fotoEstado) {
    }

    public record Item(Long id, String autorNombre, boolean autorEsAdmin, boolean propio, String texto,
                       LocalDateTime fecha, boolean automatico) {
        public static Item fromEntity(Mensaje m, Long quienMira) {
            return new Item(m.getId(), m.getAutorNombre(), m.isAutorEsAdmin(), quienMira.equals(m.getAutorId()),
                    m.getTexto(), m.getFecha(), m.isAutomatico());
        }
    }
}
