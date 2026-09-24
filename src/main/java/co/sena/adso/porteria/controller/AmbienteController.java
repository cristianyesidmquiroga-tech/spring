package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AmbienteResponseDTO;
import co.sena.adso.porteria.dto.DetalleAmbienteResponseDTO;
import co.sena.adso.porteria.service.AsistenciaService;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ambientes")
public class AmbienteController {

    private final AsistenciaService asistenciaService;

    public AmbienteController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping
    public List<AmbienteResponseDTO> activos() {
        return asistenciaService.ambientes();
    }

    @GetMapping("/{ficha}")
    public DetalleAmbienteResponseDTO detalle(@PathVariable @Size(max = 20) String ficha) {
        return asistenciaService.detalleAmbiente(ficha);
    }
}
