package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.ClaseResponseDTO;
import co.sena.adso.porteria.service.AsistenciaService;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/clases")
public class ClaseController {

    private final AsistenciaService asistenciaService;

    public ClaseController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping
    public List<ClaseResponseDTO> historial(@RequestParam(required = false) @Size(max = 20) String ficha) {
        return asistenciaService.historialClases(ficha);
    }
}
