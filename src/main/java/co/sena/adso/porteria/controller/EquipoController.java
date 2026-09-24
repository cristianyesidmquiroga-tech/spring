package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.EquipoRequestDTO;
import co.sena.adso.porteria.dto.EquipoResponseDTO;
import co.sena.adso.porteria.service.EquipoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public List<EquipoResponseDTO> misEquipos() {
        return equipoService.misEquipos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponseDTO registrar(@Valid @RequestBody EquipoRequestDTO datos) {
        return equipoService.registrar(datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        equipoService.eliminar(id);
    }
}
