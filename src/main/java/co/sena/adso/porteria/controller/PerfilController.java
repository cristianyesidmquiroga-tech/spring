package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.CarnetResponseDTO;
import co.sena.adso.porteria.dto.PerfilRequestDTO;
import co.sena.adso.porteria.dto.PerfilResponseDTO;
import co.sena.adso.porteria.service.AvatarService;
import co.sena.adso.porteria.service.PerfilService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
public class PerfilController {

    private static final MediaType SVG = MediaType.valueOf("image/svg+xml");

    private final PerfilService perfilService;
    private final AvatarService avatarService;

    public PerfilController(PerfilService perfilService, AvatarService avatarService) {
        this.perfilService = perfilService;
        this.avatarService = avatarService;
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
        PerfilService.Imagen imagen = perfilService.foto(id);
        return ResponseEntity.ok()
                .contentType(imagen.silueta() ? SVG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(imagen.archivo());
    }

    // Las siluetas no son datos personales: se sirven sin sesión y se pueden guardar en caché
    @GetMapping("/api/avatares/{cargo}")
    public ResponseEntity<Resource> avatar(@PathVariable @Size(max = 50) String cargo) {
        return ResponseEntity.ok()
                .contentType(SVG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .body(avatarService.avatar(cargo));
    }
}
