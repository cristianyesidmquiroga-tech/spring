package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.Equipo;

public record EquipoResponseDTO(Long id, String nombre, String serial, String tipo, String estado) {

    public static EquipoResponseDTO fromEntity(Equipo e) {
        return new EquipoResponseDTO(e.getId(), e.getNombre(), e.getSerial(), e.getTipo(), e.getEstado());
    }
}
