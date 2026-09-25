package co.sena.adso.porteria.dto;

import java.time.LocalDateTime;

public record HiloResponseDTO(Long usuarioId, String nombre, String cargo, String documento, boolean tieneFoto,
                              String ultimoTexto, boolean ultimoEsAdmin, LocalDateTime ultimaFecha, long sinLeer) {
}
