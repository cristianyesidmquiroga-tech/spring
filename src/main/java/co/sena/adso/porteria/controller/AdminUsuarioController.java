package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AutorizacionRequestDTO;
import co.sena.adso.porteria.dto.FotoPendienteResponseDTO;
import co.sena.adso.porteria.dto.RevisionFotoRequestDTO;
import co.sena.adso.porteria.dto.UsuarioAdminRequestDTO;
import co.sena.adso.porteria.dto.UsuarioAdminResponseDTO;
import co.sena.adso.porteria.service.UsuarioAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@Validated
public class AdminUsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    public AdminUsuarioController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping("/usuarios")
    public Page<UsuarioAdminResponseDTO> listar(
            @RequestParam(required = false) @Size(max = 100) String texto,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) @Size(max = 50) String cargo,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return usuarioAdminService.listar(texto, rolId, cargo, pageable);
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioAdminResponseDTO obtener(@PathVariable Long id) {
        return usuarioAdminService.obtener(id);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioAdminResponseDTO> crear(@Valid @RequestBody UsuarioAdminRequestDTO datos) {
        UsuarioAdminResponseDTO creado = usuarioAdminService.crear(datos);
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioAdminResponseDTO editar(@PathVariable Long id, @Valid @RequestBody UsuarioAdminRequestDTO datos) {
        return usuarioAdminService.editar(id, datos);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @Valid @RequestBody AutorizacionRequestDTO autorizacion) {
        usuarioAdminService.eliminar(id, autorizacion);
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    public UsuarioAdminResponseDTO desbloquear(@PathVariable Long id) {
        return usuarioAdminService.desbloquear(id);
    }

    @GetMapping("/fotos/pendientes")
    public List<FotoPendienteResponseDTO> fotosPendientes() {
        return usuarioAdminService.fotosPendientes();
    }

    @PostMapping("/fotos/{usuarioId}/revision")
    public UsuarioAdminResponseDTO revisarFoto(@PathVariable Long usuarioId,
                                               @Valid @RequestBody RevisionFotoRequestDTO revision) {
        return usuarioAdminService.revisarFoto(usuarioId, revision);
    }
}
