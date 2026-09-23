package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.CatalogosResponseDTO;
import co.sena.adso.porteria.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/api/catalogos")
    public CatalogosResponseDTO catalogos() {
        return catalogoService.obtener();
    }
}
