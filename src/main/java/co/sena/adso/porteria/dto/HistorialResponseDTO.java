package co.sena.adso.porteria.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record HistorialResponseDTO(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean rangoRecortado,
        boolean soloHabiles,
        List<Bloque> personas,
        List<String> omitidas
) {
    public record Bloque(Long id, String nombre, String documento, String cargo, String ficha,
                         List<Movimiento> movimientos, Resumen resumen) {
    }

    public record Movimiento(LocalDate fecha, LocalDateTime entrada, LocalDateTime salida, Long permanenciaMinutos,
                             List<String> equipos, boolean abierto, boolean cierreAutomatico,
                             boolean entradaFueraDeVentana, boolean entradaPreviaAlRango,
                             boolean salidaPosteriorAlRango) {
    }

    public record DetalleDia(int faltas, int oportunidades) {
    }

    public record Resumen(
            int diasEsperados,
            int diasAsistidos,
            int diasFaltados,
            List<LocalDate> fechasFaltadas,
            int diasFueraDeComputo,
            Double porcentajeAsistencia,
            LocalDate periodoEvaluadoInicio,
            LocalDate periodoEvaluadoFin,
            List<String> motivosVentana,
            Map<String, Integer> faltasPorDia,
            Map<String, DetalleDia> detalleDiasSemana,
            String diaMasFaltado,
            List<String> diasEmpatados,
            String motivoSinDia,
            int totalMovimientos,
            int sinSalida,
            int abiertos,
            Long promedioPermanenciaMinutos
    ) {
    }
}
