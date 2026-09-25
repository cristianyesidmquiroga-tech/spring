package co.sena.adso.porteria.dto;

import java.util.List;

public record AyudaResponseDTO(List<Categoria> categorias, List<String> asuntos) {

    public record Pregunta(String pregunta, String respuesta) {
    }

    public record Categoria(String nombre, List<Pregunta> preguntas) {
    }
}
