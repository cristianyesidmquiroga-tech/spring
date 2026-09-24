package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.FichaRequestDTO;
import co.sena.adso.porteria.dto.FichaResponseDTO;
import co.sena.adso.porteria.service.FichaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/fichas")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping
    public List<FichaResponseDTO> listar() {
        return fichaService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FichaResponseDTO crear(@Valid @RequestBody FichaRequestDTO datos) {
        return fichaService.crear(datos);
    }

    @PutMapping("/{id}")
    public FichaResponseDTO editar(@PathVariable Long id, @Valid @RequestBody FichaRequestDTO datos) {
        return fichaService.editar(id, datos);
    }

    @PatchMapping("/{id}/archivar")
    public FichaResponseDTO archivar(@PathVariable Long id) {
        return fichaService.alternarArchivo(id);
    }
}
