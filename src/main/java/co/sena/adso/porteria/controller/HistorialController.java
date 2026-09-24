package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.HistorialResponseDTO;
import co.sena.adso.porteria.service.HistorialService;
import co.sena.adso.porteria.service.HistorialService.Filtros;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    // Los permisos se revisan en el servicio: cualquiera ve el suyo, portería e instructores ven el de otros
    @GetMapping("/api/historial")
    public HistorialResponseDTO historial(
            @RequestParam(required = false) @Size(max = 50) List<Long> usuarioId,
            @RequestParam(required = false) @Size(max = 20) String ficha,
            @RequestParam(required = false) @Size(max = 50) String cargo,
            @RequestParam(required = false) @Size(max = 100) String busqueda,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "true") boolean soloHabiles) {
        return historialService.consultar(new Filtros(usuarioId, ficha, cargo, busqueda, desde, hasta, soloHabiles));
    }
}
