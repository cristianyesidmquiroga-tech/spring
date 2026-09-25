package co.sena.adso.porteria.dto;

import java.util.List;

public record ImportacionResponseDTO(String mensaje, Detalles detalles) {

    public record Detalles(int creados, int omitidos, List<String> errores) {
    }
}
