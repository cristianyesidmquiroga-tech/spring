package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.AyudaResponseDTO;
import co.sena.adso.porteria.dto.AyudaResponseDTO.Categoria;
import co.sena.adso.porteria.dto.AyudaResponseDTO.Pregunta;
import co.sena.adso.porteria.dto.ContactoRequestDTO;
import co.sena.adso.porteria.dto.MensajeResponseDTO;
import co.sena.adso.porteria.entity.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Preguntas frecuentes y puente con un asesor, en orden de lo que más bloquea a la persona
@Service
public class AyudaService {

    public static final List<String> ASUNTOS = List.of(
            "No puedo activar mi carnet",
            "Problema con mi foto de perfil",
            "Mis datos están mal (ficha, programa, documento)",
            "Problema para iniciar sesión",
            "Problema con el registro de un equipo",
            "Otro");

    private static final List<Categoria> CATEGORIAS = List.of(
            new Categoria("Carnet digital", List.of(
                    new Pregunta("¿Por qué no me aparece el código de barras?",
                            "El código de barras aparece solo cuando tu perfil está completo Y tu foto fue aprobada por "
                                    + "un asesor. Revisa en \"Mi Perfil\" que tengas: documento, tipo de sangre, foto, y si "
                                    + "eres aprendiz también tu ficha de formación (el programa y la fecha de finalización "
                                    + "se heredan de ella). Si ya lo tienes todo, tu foto puede estar todavía en revisión."),
                    new Pregunta("¿Cuánto tarda en revisarse mi foto?",
                            "La revisa un asesor manualmente, así que depende de la carga del día. Mientras tanto puedes "
                                    + "entrar al centro presentando tu documento físico en portería. Si llevas varios días "
                                    + "esperando, escríbenos."))),
            new Categoria("Foto de perfil", List.of(
                    new Pregunta("¿Cómo debe ser mi foto?",
                            "Debe salir solo tu rostro, de frente y de cerca, tipo foto de documento. Con buena luz, sin "
                                    + "contraluz y sin flash directo. Sin gorra, capucha, mascarilla ni bufanda que te tape "
                                    + "la cara. Puedes usar gafas o lentes si los llevas normalmente. Puedes tomarla en el "
                                    + "momento o subir una de tu galería, pero tiene que ser una foto real tuya."),
                    new Pregunta("El sistema me rechaza la foto, ¿qué hago?",
                            "El mensaje de error te dice qué corregir. Los casos más comunes son: la foto está movida "
                                    + "(apoya el celular y enfoca), está muy oscura (busca un lugar claro y ponte de frente "
                                    + "a la luz), sale más de una persona, o el rostro se ve muy pequeño (acércate más). Si "
                                    + "no logras que la acepte, escríbenos y te ayudamos."),
                    new Pregunta("Un asesor rechazó mi foto, ¿por qué?",
                            "El motivo te llega por correo y también queda en tus Mensajes dentro del sistema. Ahí mismo "
                                    + "puedes responder si no entiendes qué corregir. La foto rechazada se borra, así que "
                                    + "tienes que subir una nueva."))),
            new Categoria("Mis datos", List.of(
                    new Pregunta("Mi ficha o mi programa están mal, ¿cómo los corrijo?",
                            "Puedes editarlos tú desde \"Mi Perfil\" → \"Información\". Si el sistema no te deja porque "
                                    + "dice que hay una inconsistencia con la ficha, escríbenos por Mensajes: eso significa "
                                    + "que otro aprendiz de tu misma ficha tiene un programa u horario distinto y hay que "
                                    + "revisarlo."),
                    new Pregunta("¿Qué número de documento debo poner?",
                            "El de tu documento de identidad, sin puntos ni espacios, y elige el tipo que corresponda: "
                                    + "cédula de ciudadanía, tarjeta de identidad si eres menor de edad, cédula de "
                                    + "extranjería, PPT o pasaporte. El sistema valida que la cantidad de dígitos sea la "
                                    + "correcta para ese tipo."))),
            new Categoria("Equipos", List.of(
                    new Pregunta("¿Tengo que registrar mi computador?",
                            "Sí, si vas a entrar con un portátil, tablet u otro equipo tecnológico, regístralo antes en "
                                    + "\"Mi Perfil\" → \"Añadir Equipos\". En portería el celador marca cuáles traes contigo "
                                    + "al entrar y al salir. Puedes registrar hasta 5 equipos."))),
            new Categoria("Acceso", List.of(
                    new Pregunta("No puedo iniciar sesión, dice que estoy bloqueado",
                            "Tras 5 intentos fallidos la cuenta se bloquea 10 minutos por seguridad. Espera ese tiempo y "
                                    + "vuelve a intentar. Si no recuerdas tu contraseña, usa \"¿Olvidaste tu contraseña?\" "
                                    + "en la pantalla de ingreso."),
                    new Pregunta("No me llega el correo de verificación",
                            "Revisa la carpeta de spam o correo no deseado. Desde la pantalla de verificación puedes "
                                    + "pedir que te reenvíen el código. Si tu correo quedó mal escrito al registrarte, "
                                    + "escríbenos para corregirlo."))),
            new Categoria("Privacidad", List.of(
                    new Pregunta("¿Qué hacen con mi foto y mis datos?",
                            "Se usan únicamente para controlar el ingreso a la sede y para que el celador confirme tu "
                                    + "identidad en portería. No se comparten con terceros. Puedes consultar el detalle en "
                                    + "la Política de Tratamiento de Datos Personales, enlazada al final de cualquier "
                                    + "página."))));

    private final MensajeService mensajeService;
    private final AuthService authService;

    public AyudaService(MensajeService mensajeService, AuthService authService) {
        this.mensajeService = mensajeService;
        this.authService = authService;
    }

    public AyudaResponseDTO consultar() {
        return new AyudaResponseDTO(CATEGORIAS, ASUNTOS);
    }

    // El asunto va dentro del texto para que el asesor sepa de qué se trata sin preguntar
    @Transactional
    public MensajeResponseDTO contactar(ContactoRequestDTO datos) {
        String detalle = MensajeService.limpio(datos.detalle());
        String asunto = Texto.opcional(datos.asunto());
        String texto = asunto != null && ASUNTOS.contains(asunto) && !"Otro".equals(asunto)
                ? "[" + asunto + "]\n\n" + detalle : detalle;
        Usuario yo = authService.usuarioActual();
        mensajeService.registrar(yo.getId(), yo, texto, false);
        return new MensajeResponseDTO("Tu mensaje fue enviado. Un asesor te responderá en Mensajes.");
    }
}
