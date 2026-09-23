package co.sena.adso.porteria.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public Map<String, String> hola() {
        return Map.of("mensaje", "API de portería funcionando");
    }
}
