package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.ComunicadoRequestDTO;
import co.sena.adso.porteria.dto.ComunicadosResponseDTO;
import co.sena.adso.porteria.dto.EnvioComunicadoResponseDTO;
import co.sena.adso.porteria.service.ComunicadoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comunicados")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    public ComunicadoController(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    @GetMapping
    public ComunicadosResponseDTO consultar() {
        return comunicadoService.consultar();
    }

    @PostMapping
    public EnvioComunicadoResponseDTO enviar(@Valid @RequestBody ComunicadoRequestDTO datos) {
        return comunicadoService.enviar(datos);
    }
}
