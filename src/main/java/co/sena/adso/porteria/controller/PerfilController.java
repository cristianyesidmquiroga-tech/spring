package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.CarnetResponseDTO;
import co.sena.adso.porteria.dto.PerfilRequestDTO;
import co.sena.adso.porteria.dto.PerfilResponseDTO;
import co.sena.adso.porteria.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/api/perfil")
    public PerfilResponseDTO obtener() {
        return perfilService.obtener();
    }

    @PutMapping("/api/perfil")
    public PerfilResponseDTO actualizar(@Valid @RequestBody PerfilRequestDTO datos) {
        return perfilService.actualizar(datos);
    }

    @PostMapping(value = "/api/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PerfilResponseDTO subirFoto(@RequestParam("foto") MultipartFile foto) {
        return perfilService.subirFoto(foto);
    }

    @GetMapping("/api/perfil/carnet")
    public CarnetResponseDTO carnet() {
        return perfilService.carnet();
    }

    // La foto no se cachea en disco del navegador: es un dato personal
    @GetMapping("/api/usuarios/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(perfilService.foto(id));
    }
}
