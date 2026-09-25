package co.sena.adso.porteria.dto;

import java.util.List;

public record TutorialResponseDTO(boolean visto, List<Paso> pasos) {

    public record Paso(String titulo, String icono, String descripcion, String ejemplo) {
    }
}
