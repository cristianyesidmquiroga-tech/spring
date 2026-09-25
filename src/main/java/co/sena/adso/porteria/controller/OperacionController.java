package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AuditoriaResponseDTO;
import co.sena.adso.porteria.dto.ImportacionResponseDTO;
import co.sena.adso.porteria.service.AuditoriaService;
import co.sena.adso.porteria.service.CorreoService;
import co.sena.adso.porteria.service.ImportacionService;
import co.sena.adso.porteria.service.RespaldoService;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
public class OperacionController {

    private final AuditoriaService auditoriaService;
    private final RespaldoService respaldoService;
    private final ImportacionService importacionService;
    private final CorreoService correoService;
    private final String centro;
    private final String regional;

    public OperacionController(AuditoriaService auditoriaService, RespaldoService respaldoService,
                               ImportacionService importacionService, CorreoService correoService,
                               @Value("${app.carnet.centro}") String centro, @Value("${app.carnet.regional}") String regional) {
        this.auditoriaService = auditoriaService;
        this.respaldoService = respaldoService;
        this.importacionService = importacionService;
        this.correoService = correoService;
        this.centro = centro;
        this.regional = regional;
    }

    @GetMapping("/api/admin/auditoria")
    public Page<AuditoriaResponseDTO> auditoria(@RequestParam(defaultValue = "0") @Min(0) int pagina) {
        return auditoriaService.listar(pagina);
    }

    @GetMapping("/api/admin/respaldos")
    public List<String> respaldos() {
        return respaldoService.listar();
    }

    @GetMapping("/api/admin/respaldos/{archivo}")
    public ResponseEntity<Resource> descargarRespaldo(@PathVariable String archivo) {
        Resource recurso = respaldoService.archivo(archivo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(recurso);
    }

    @PostMapping(value = "/api/admin/usuarios/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacionResponseDTO importar(@RequestParam("archivo") MultipartFile archivo) {
        return importacionService.importar(archivo);
    }

    // El envío va en cola: el fallo real solo se ve aquí o en el log del servidor
    @GetMapping("/api/admin/correos/fallidos")
    public List<Map<String, String>> correosFallidos() {
        return correoService.fallosRecientes();
    }

    @GetMapping("/api/politica-privacidad")
    public Map<String, String> politica() {
        return Map.of("entidad", "SENA", "entidadLarga", "Servicio Nacional de Aprendizaje", "centro", centro,
                "regional", regional);
    }
}
