package co.sena.adso.fincasapi.controller;

import co.sena.adso.fincasapi.dto.CultivoRequestDTO;
import co.sena.adso.fincasapi.dto.CultivoResponseDTO;
import co.sena.adso.fincasapi.service.CultivoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cultivos")
public class CultivoController {

    private final CultivoService cultivoService;

    public CultivoController(CultivoService cultivoService) {
        this.cultivoService = cultivoService;
    }

    @GetMapping
    public List<CultivoResponseDTO> listar() {
        return cultivoService.listar();
    }

    @GetMapping("/{id}")
    public CultivoResponseDTO obtener(@PathVariable Long id) {
        return cultivoService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CultivoResponseDTO> crear(@Valid @RequestBody CultivoRequestDTO datos) {
        CultivoResponseDTO creado = cultivoService.crear(datos);
        return ResponseEntity.created(URI.create("/api/cultivos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public CultivoResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody CultivoRequestDTO datos) {
        return cultivoService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        cultivoService.eliminar(id);
    }
}
