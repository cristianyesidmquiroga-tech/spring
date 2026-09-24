package co.sena.adso.porteria.dto;

import co.sena.adso.porteria.entity.ObjetoExterno;
import co.sena.adso.porteria.entity.Vehiculo;
import co.sena.adso.porteria.entity.Visitante;
import java.time.LocalDateTime;
import java.util.List;

public record PasesResponseDTO(List<VisitanteDTO> visitantes, List<VehiculoDTO> vehiculos, List<ObjetoDTO> objetos) {

    public record VisitanteDTO(Long id, String nombre, String documento, String motivo, String codigo,
                               boolean activo, LocalDateTime fechaCreacion) {
        public static VisitanteDTO fromEntity(Visitante v) {
            return new VisitanteDTO(v.getId(), v.getNombre(), v.getDocumento(), v.getMotivo(), v.getCodigo(),
                    v.isActivo(), v.getFechaCreacion());
        }
    }

    public record VehiculoDTO(Long id, String placa, String tipo, String propietario, String motivo, String codigo,
                              boolean activo, LocalDateTime fechaCreacion) {
        public static VehiculoDTO fromEntity(Vehiculo v) {
            return new VehiculoDTO(v.getId(), v.getPlaca(), v.getTipo(), v.getPropietario(), v.getMotivo(),
                    v.getCodigo(), v.isActivo(), v.getFechaCreacion());
        }
    }

    public record ObjetoDTO(Long id, String descripcion, String serial, String propietario, String motivo, String codigo,
                            boolean activo, LocalDateTime fechaCreacion) {
        public static ObjetoDTO fromEntity(ObjetoExterno o) {
            return new ObjetoDTO(o.getId(), o.getDescripcion(), o.getSerial(), o.getPropietario(), o.getMotivo(),
                    o.getCodigo(), o.isActivo(), o.getFechaCreacion());
        }
    }
}
