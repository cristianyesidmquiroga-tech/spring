package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;

public record AsistenciaResponseDTO(String ficha, List<Aprendiz> aprendices) {

    public record Aprendiz(Long id, String nombre, String documento, String programa, String horario,
                           LocalDateTime llegada) {
        public static Aprendiz fromEntity(Usuario u, LocalDateTime llegada) {
            return new Aprendiz(u.getId(), u.getNombre(), u.getDocumento(), u.programaCarnet(), u.getHorario(), llegada);
        }
    }
}
