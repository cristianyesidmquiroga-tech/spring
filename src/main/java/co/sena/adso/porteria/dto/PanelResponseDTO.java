package co.sena.adso.porteria.dto;

import java.util.List;

public record PanelResponseDTO(
        Indicadores indicadores,
        Grafica grafica,
        String analisis,
        List<String> cargos,
        List<String> fichas
) {
    public record Indicadores(long aprendices, long instructores, long trabajadores,
                              long visitantesAdentro, long vehiculosAdentro, long objetosAdentro) {
    }

    public record Grafica(List<String> dias, List<Long> aprendices, List<Long> instructores, List<Long> otros) {
    }
}
