package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AsistenciaRequestDTO;
import co.sena.adso.porteria.dto.AsistenciaResponseDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.service.AsistenciaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/asistencia")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping
    public AsistenciaResponseDTO buscar(@RequestParam(required = false) @Size(max = 20) String ficha) {
        return asistenciaService.buscar(ficha);
    }

    @PostMapping
    public MensajeResponseDTO guardar(@Valid @RequestBody AsistenciaRequestDTO datos) {
        return asistenciaService.guardar(datos);
    }
}
