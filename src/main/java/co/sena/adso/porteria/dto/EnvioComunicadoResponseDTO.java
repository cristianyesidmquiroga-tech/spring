package co.sena.adso.porteria.dto;

import java.util.List;

public record EnvioComunicadoResponseDTO(String mensaje, int enviados, List<String> errores) {
}
