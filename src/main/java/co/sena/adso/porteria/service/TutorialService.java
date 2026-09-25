package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.dto.TutorialResponseDTO;
import co.sena.adso.porteria.dto.TutorialResponseDTO.Paso;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Versión en texto del recorrido guiado: explica que el código de barras no se activa solo
@Service
public class TutorialService {

    private static final List<Paso> PASOS = List.of(
            new Paso("Completa tu información personal", "fa-user-edit",
                    "En \"Mi Perfil\", presiona el botón \"Información\" y diligencia todos los campos: tipo y número "
                            + "de documento, tipo de sangre, y si eres aprendiz también tu programa de formación y número "
                            + "de ficha. Sin estos datos el carnet digital no se puede activar.",
                    "Documento: elige \"Cédula de ciudadanía\" y escribe el número sin puntos ni espacios, por ejemplo "
                            + "1098765432 (no 1.098.765.432). Ficha: el número que aparece en tu carta de aceptación, "
                            + "por ejemplo 2758291."),
            new Paso("Sube tu foto de perfil", "fa-camera",
                    "En el mismo formulario de \"Información\" sube una foto tuya. En portería el celador la compara "
                            + "contigo para dejarte entrar, así que debe cumplir los requisitos: solo tú en la imagen, "
                            + "rostro despejado y bien visible, con buena luz, de frente y de cerca.",
                    "Sirve: una foto tipo documento, tomada de frente en un lugar iluminado, con gafas si las usas "
                            + "normalmente. No sirve: una foto con gorra o mascarilla, a contraluz, movida, o donde "
                            + "aparezca alguien más aunque sea de fondo."),
            new Paso("Espera la aprobación de un asesor", "fa-user-check",
                    "Un asesor revisa tu foto manualmente. Hasta que la apruebe, tu carnet digital y tu código de "
                            + "barras permanecen bloqueados. Si la rechaza, el motivo te llega por correo y a tus "
                            + "Mensajes, y debes subir una nueva.",
                    "Mientras esperas puedes entrar al centro presentando tu documento físico en portería. Si llevas "
                            + "varios días sin respuesta, escribe por \"Mensajes\" y un asesor te atiende."),
            new Paso("Registra tus equipos", "fa-laptop",
                    "Si vas a entrar con portátil, tablet u otro equipo tecnológico, regístralo antes en \"Mi Perfil\" "
                            + "→ \"Añadir Equipos\". El celador marca cuáles traes al entrar y al salir. Puedes "
                            + "registrar hasta 5.",
                    "Serial: el código que está en la etiqueta de la parte de abajo del portátil, por ejemplo "
                            + "5CD1234XYZ. Nombre: algo que lo identifique, como \"Portátil HP negro\"."),
            new Paso("Usa tu código de barras en portería", "fa-qrcode",
                    "Cuando tu perfil esté completo y tu foto aprobada, el código de barras aparece en tu carnet "
                            + "digital, en \"Mi Perfil\". Preséntalo al escáner de portería al entrar y al salir del "
                            + "centro.",
                    "Toca el código de barras de tu carnet para agrandarlo y acércalo al lector con la pantalla con "
                            + "buen brillo. También puedes descargar el carnet como imagen con el botón \"Descargar "
                            + "Carnet Institucional\"."),
            new Paso("¿Algo falla? Tienes ayuda dentro del sistema", "fa-circle-question",
                    "En el \"Centro de Ayuda\" del menú están las respuestas a las dudas más comunes (foto rechazada, "
                            + "código de barras que no aparece, ficha mal escrita). Y en \"Mensajes\" puedes hablar "
                            + "directamente con un asesor sin tener que buscar a nadie en portería.",
                    "Ejemplo: si el sistema rechaza tu foto y no entiendes el motivo, abre \"Centro de Ayuda\", elige "
                            + "el asunto \"Problema con mi foto de perfil\" y cuenta qué te sale. La respuesta llega a "
                            + "\"Mensajes\"."));

    private final AuthService authService;

    public TutorialService(AuthService authService) {
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public TutorialResponseDTO consultar() {
        return new TutorialResponseDTO(authService.usuarioActual().isTutorialVisto(), PASOS);
    }

    // Siempre sobre quien tiene la sesión: no hay forma de marcar el de otra persona
    @Transactional
    public MensajeResponseDTO completar() {
        authService.usuarioActual().marcarTutorialVisto();
        return new MensajeResponseDTO("Tutorial marcado como visto");
    }
}
