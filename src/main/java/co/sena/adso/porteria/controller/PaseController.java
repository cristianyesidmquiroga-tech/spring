package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.ObjetoRequestDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.ObjetoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VehiculoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VisitanteDTO;
import co.sena.adso.porteria.dto.VehiculoRequestDTO;
import co.sena.adso.porteria.dto.VisitanteRequestDTO;
import co.sena.adso.porteria.service.CodigoBarrasService;
import co.sena.adso.porteria.service.PaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/porteria/pases")
@PreAuthorize("hasAuthority('OPERAR_PORTERIA')")
@Validated
public class PaseController {

    private final PaseService paseService;
    private final CodigoBarrasService codigoBarras;

    public PaseController(PaseService paseService, CodigoBarrasService codigoBarras) {
        this.paseService = paseService;
        this.codigoBarras = codigoBarras;
    }

    @GetMapping
    public PasesResponseDTO listar() {
        return paseService.listar();
    }

    // Código del pase para imprimir o fotografiar; lo lee el escáner de portería
    @GetMapping(value = "/codigo", produces = "image/svg+xml")
    public String codigo(@RequestParam @NotBlank @Size(max = 100) String texto) {
        return codigoBarras.svg(texto);
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
