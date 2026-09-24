package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.AsistenciaClase;
import co.sena.adso.porteria.entity.Usuario;
import java.time.LocalDateTime;

public record ClaseResponseDTO(Long id, LocalDateTime fecha, String ficha, String programa, String horario,
                               String aprendiz, String documento, String instructor, boolean presente) {

    public static ClaseResponseDTO fromEntity(AsistenciaClase a) {
        Usuario aprendiz = a.getAprendiz();
        Usuario instructor = a.getInstructor();
        return new ClaseResponseDTO(a.getId(), a.getFecha(), a.getFicha(), aprendiz.programaCarnet(),
                aprendiz.getHorario(), aprendiz.getNombre(), aprendiz.getDocumento(),
                instructor != null ? instructor.getNombre() : null, a.isPresente());
    }
}
