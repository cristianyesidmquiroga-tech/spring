package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.IncidenteRequestDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.MovimientoRequestDTO;
import co.sena.adso.porteria.dto.VerificacionResponseDTO;
import co.sena.adso.porteria.service.PorteriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/porteria")
@PreAuthorize("hasAuthority('OPERAR_PORTERIA')")
@Validated
public class PorteriaController {

    private final PorteriaService porteriaService;

    public PorteriaController(PorteriaService porteriaService) {
        this.porteriaService = porteriaService;
    }

    @GetMapping("/verificar")
    public VerificacionResponseDTO verificar(@RequestParam @NotBlank @Size(max = 100) String codigo) {
        return porteriaService.verificar(codigo);
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO registrarMovimiento(@Valid @RequestBody MovimientoRequestDTO datos) {
        return porteriaService.registrarMovimiento(datos);
    }

    @PostMapping("/incidentes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO registrarIncidente(@Valid @RequestBody IncidenteRequestDTO datos) {
        return porteriaService.registrarIncidente(datos);
    }
}
