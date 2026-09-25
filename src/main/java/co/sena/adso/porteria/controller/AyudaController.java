package co.sena.adso.porteria.controller;

import co.sena.adso.porteria.dto.AyudaResponseDTO;
import co.sena.adso.porteria.dto.ContactoRequestDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.TutorialResponseDTO;
import co.sena.adso.porteria.service.AyudaService;
import co.sena.adso.porteria.service.TutorialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AyudaController {

    private final AyudaService ayudaService;
    private final TutorialService tutorialService;

    public AyudaController(AyudaService ayudaService, TutorialService tutorialService) {
        this.ayudaService = ayudaService;
        this.tutorialService = tutorialService;
    }

    @GetMapping("/api/ayuda")
    public AyudaResponseDTO ayuda() {
        return ayudaService.consultar();
    }

    @PostMapping("/api/ayuda/contacto")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponseDTO contactar(@Valid @RequestBody ContactoRequestDTO datos) {
        return ayudaService.contactar(datos);
    }

    @GetMapping("/api/tutorial")
    public TutorialResponseDTO tutorial() {
        return tutorialService.consultar();
    }

    @PostMapping("/api/tutorial/completar")
    public MensajeResponseDTO completarTutorial() {
        return tutorialService.completar();
    }
}
