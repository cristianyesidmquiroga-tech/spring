package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.PanelResponseDTO;
import co.sena.adso.porteria.dto.RegistroAccesoDTO;
import co.sena.adso.porteria.dto.ReporteCargoResponseDTO;
import co.sena.adso.porteria.service.PanelService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/porteria/panel")
@PreAuthorize("hasAuthority('OPERAR_PORTERIA')")
@Validated
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping
    public PanelResponseDTO panel() {
        return panelService.panel();
    }

    @GetMapping("/accesos")
    public Page<RegistroAccesoDTO> accesos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @Size(max = 50) String cargo,
            @RequestParam(required = false) @Size(max = 20) String ficha,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int pagina) {
        return panelService.recientes(desde, hasta, cargo, ficha, pagina);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @Size(max = 50) String cargo,
            @RequestParam(required = false) @Size(max = 20) String ficha) {
        byte[] csv = panelService.exportar(desde, hasta, vacioANull(cargo), vacioANull(ficha))
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("accesos_" + desde + "_" + hasta + ".csv").build().toString())
                .body(csv);
    }

    @GetMapping("/reportes/{cargo}")
    public ReporteCargoResponseDTO reporte(@PathVariable @Size(max = 50) String cargo) {
        return panelService.reportePorCargo(cargo);
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
