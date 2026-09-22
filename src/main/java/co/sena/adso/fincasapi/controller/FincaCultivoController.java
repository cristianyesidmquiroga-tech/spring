package co.sena.adso.fincasapi.controller;

import co.sena.adso.fincasapi.dto.FincaCultivoRequestDTO;
import co.sena.adso.fincasapi.dto.FincaCultivoResponseDTO;
import co.sena.adso.fincasapi.service.FincaCultivoService;
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
@RequestMapping("/api/finca-cultivos")
public class FincaCultivoController {

    private final FincaCultivoService siembraService;

    public FincaCultivoController(FincaCultivoService siembraService) {
        this.siembraService = siembraService;
    }

    @GetMapping
    public List<FincaCultivoResponseDTO> listar() {
        return siembraService.listar();
    }

    @GetMapping("/finca/{fincaId}")
    public List<FincaCultivoResponseDTO> listarPorFinca(@PathVariable Long fincaId) {
        return siembraService.listarPorFinca(fincaId);
    }

    @GetMapping("/{id}")
    public FincaCultivoResponseDTO obtener(@PathVariable Long id) {
        return siembraService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<FincaCultivoResponseDTO> crear(@Valid @RequestBody FincaCultivoRequestDTO datos) {
        FincaCultivoResponseDTO creada = siembraService.crear(datos);
        return ResponseEntity.created(URI.create("/api/finca-cultivos/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public FincaCultivoResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody FincaCultivoRequestDTO datos) {
        return siembraService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        siembraService.eliminar(id);
    }
}
