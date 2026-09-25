package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AvisosResponseDTO;
import co.sena.adso.porteria.dto.ConversacionResponseDTO;
import co.sena.adso.porteria.dto.HiloResponseDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.TextoMensajeRequestDTO;
import co.sena.adso.porteria.service.MensajeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @GetMapping("/api/mensajes")
    public ConversacionResponseDTO misMensajes() {
        return mensajeService.misMensajes();
    }

    @PostMapping("/api/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO enviar(@Valid @RequestBody TextoMensajeRequestDTO datos) {
        return mensajeService.enviar(datos.texto());
    }

    @GetMapping("/api/avisos")
    public AvisosResponseDTO avisos() {
        return mensajeService.avisos();
    }

    @GetMapping("/api/bandeja")
    public List<HiloResponseDTO> bandeja() {
        return mensajeService.bandeja();
    }

    @GetMapping("/api/bandeja/{usuarioId}")
    public ConversacionResponseDTO hilo(@PathVariable Long usuarioId) {
        return mensajeService.hilo(usuarioId);
    }

    @PostMapping("/api/bandeja/{usuarioId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO responder(@PathVariable Long usuarioId, @Valid @RequestBody TextoMensajeRequestDTO datos) {
        return mensajeService.responder(usuarioId, datos.texto());
    }
}
