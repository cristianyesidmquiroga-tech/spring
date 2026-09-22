package co.sena.adso.fincasapi.controller;

import co.sena.adso.fincasapi.dto.FincaRequestDTO;
import co.sena.adso.fincasapi.dto.FincaResponseDTO;
import co.sena.adso.fincasapi.service.FincaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fincas")
public class FincaController {

    private final FincaService fincaService;

    public FincaController(FincaService fincaService) {
        this.fincaService = fincaService;
    }

    @GetMapping
    public List<FincaResponseDTO> listar() {
        return fincaService.listar();
    }

    @GetMapping("/paginado")
    public Page<FincaResponseDTO> listarPaginado(
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return fincaService.listarPaginado(pageable);
    }

    @GetMapping("/buscar")
    public List<FincaResponseDTO> buscar(@RequestParam(required = false) String municipio,
                                      @RequestParam(required = false) String propietario,
                                      @RequestParam(required = false) Double hectareasMin) {
        return fincaService.buscar(municipio, propietario, hectareasMin);
    }

    @GetMapping("/{id}")
    public FincaResponseDTO obtener(@PathVariable Long id) {
        return fincaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<FincaResponseDTO> crear(@Valid @RequestBody FincaRequestDTO datos) {
        FincaResponseDTO creada = fincaService.crear(datos);
        return ResponseEntity.created(URI.create("/api/fincas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public FincaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody FincaRequestDTO datos) {
        return fincaService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        fincaService.eliminar(id);
    }
}
