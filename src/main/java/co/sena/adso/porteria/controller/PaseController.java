package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.ObjetoRequestDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.ObjetoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VehiculoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VisitanteDTO;
import co.sena.adso.porteria.dto.VehiculoRequestDTO;
import co.sena.adso.porteria.dto.VisitanteRequestDTO;
import co.sena.adso.porteria.service.PaseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/porteria/pases")
@PreAuthorize("hasAuthority('OPERAR_PORTERIA')")
public class PaseController {

    private final PaseService paseService;

    public PaseController(PaseService paseService) {
        this.paseService = paseService;
    }

    @GetMapping
    public PasesResponseDTO listar() {
        return paseService.listar();
    }

    @PostMapping("/visitantes")
    public VisitanteDTO registrarVisitante(@Valid @RequestBody VisitanteRequestDTO datos) {
        return paseService.registrarVisitante(datos);
    }

    @PostMapping("/vehiculos")
    public VehiculoDTO registrarVehiculo(@Valid @RequestBody VehiculoRequestDTO datos) {
        return paseService.registrarVehiculo(datos);
    }

    @PostMapping("/objetos")
    public ObjetoDTO registrarObjeto(@Valid @RequestBody ObjetoRequestDTO datos) {
        return paseService.registrarObjeto(datos);
    }

    @PutMapping("/objetos/{id}")
    public ObjetoDTO actualizarObjeto(@PathVariable Long id, @Valid @RequestBody ObjetoRequestDTO datos) {
        return paseService.actualizarObjeto(id, datos);
    }

    @PostMapping("/objetos/{id}/desactivar")
    public ObjetoDTO desactivarObjeto(@PathVariable Long id) {
        return paseService.desactivarObjeto(id);
    }
}
