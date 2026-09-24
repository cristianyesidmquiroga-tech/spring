package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.ComunicadoRequestDTO;
import co.sena.adso.porteria.dto.ComunicadosResponseDTO;
import co.sena.adso.porteria.dto.ComunicadosResponseDTO.Destinatario;
import co.sena.adso.porteria.dto.ComunicadosResponseDTO.Inasistente;
import co.sena.adso.porteria.dto.EnvioComunicadoResponseDTO;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.DatoInvalidoException;
import co.sena.adso.porteria.repository.AsistenciaClaseRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
public class ComunicadoService {

    public static final Map<String, String> COLORES = Map.of(
            "Llegada tarde", "#e67e22",
            "Llamado de atención", "#e74c3c",
            "Uniforme incorrecto", "#8e44ad",
            "Comunicado General", "#2c3e50",
            "Inasistencias", "#c0392b");

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' hh:mm a", ES);

    private final UsuarioRepository usuarioRepository;
    private final AsistenciaClaseRepository asistenciaRepository;
    private final CorreoService correoService;
    private final AuthService authService;
    private final Clock reloj;
    private final String regional;
    private final String centro;

    public ComunicadoService(UsuarioRepository usuarioRepository, AsistenciaClaseRepository asistenciaRepository,
                             CorreoService correoService, AuthService authService, Clock reloj,
                             @Value("${app.carnet.regional}") String regional,
                             @Value("${app.carnet.centro}") String centro) {
        this.usuarioRepository = usuarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.correoService = correoService;
        this.authService = authService;
        this.reloj = reloj;
        this.regional = regional;
        this.centro = centro;
    }

    @Transactional(readOnly = true)
    public ComunicadosResponseDTO consultar() {
        LocalDate hoy = LocalDate.now(reloj);
        List<Destinatario> usuarios = usuarioRepository.findByCorreoVerificadoTrueOrderByCargoAscNombreAsc().stream()
                .map(u -> new Destinatario(u.getId(), u.getNombre(), u.getDocumento(), u.getCorreo(), u.getCargo(),
                        u.numeroFicha()))
                .toList();

        Map<Long, Long> faltas = faltasDelMes(hoy);
        List<Inasistente> inasistentes = usuarioRepository.findAllById(faltas.keySet()).stream()
                .map(u -> new Inasistente(u.getId(), u.getNombre(), u.getDocumento(), u.getCorreo(), u.numeroFicha(),
                        u.programaCarnet(), faltas.get(u.getId())))
                .sorted(Comparator.comparingLong(Inasistente::faltas).reversed().thenComparing(Inasistente::nombre))
                .toList();

        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, ES);
        String mesActual = Character.toUpperCase(mes.charAt(0)) + mes.substring(1) + " " + hoy.getYear();
        return new ComunicadosResponseDTO(mesActual, usuarios, inasistentes);
    }

    // Sin transacción: el envío por SMTP es lento y no debe retener la conexión a la base
    public EnvioComunicadoResponseDTO enviar(ComunicadoRequestDTO datos) {
        String tipo = datos.tipo().trim();
        if (!COLORES.containsKey(tipo)) {
            throw new DatoInvalidoException("Tipo de aviso no válido");
        }
        Usuario remitente = authService.usuarioActual();
        String mensaje = Texto.opcional(datos.mensaje());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Map<Long, Long> faltas = "Inasistencias".equals(tipo) ? faltasDelMes(ahora.toLocalDate()) : Map.of();

        Map<Long, Usuario> personas = usuarioRepository.findAllById(datos.destinatarios()).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        int enviados = 0;
        List<String> errores = new ArrayList<>();
        for (Long id : datos.destinatarios().stream().distinct().toList()) {
            Usuario u = personas.get(id);
            if (u == null) {
                continue;
            }
            String cuerpo = cuerpo(tipo, u, remitente, mensaje, faltas.getOrDefault(id, 0L), ahora);
            if (correoService.enviar(u.getCorreo(), "Comunicado SENA - " + tipo, cuerpo)) {
                enviados++;
            } else {
                errores.add(u.getNombre());
            }
        }
        return new EnvioComunicadoResponseDTO("Comunicado enviado a " + enviados + " destinatario(s).", enviados, errores);
    }

    private Map<Long, Long> faltasDelMes(LocalDate hoy) {
        return asistenciaRepository.faltasDesde(hoy.withDayOfMonth(1).atStartOfDay()).stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1]));
    }

    private static String e(String texto) {
        return HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    // Todo lo que viene de personas se escapa: el correo es HTML
    private String cuerpo(String tipo, Usuario destino, Usuario remitente, String mensaje, long faltas,
                          LocalDateTime fecha) {
        String color = COLORES.get(tipo);
        String t = e(tipo);
        String tipoMayusculas = e(tipo.toUpperCase(ES));
        String bloqueFaltas = faltas > 0 ? """
                <div style="background:#fff3cd;border-left:4px solid #f39c12;padding:12px 16px;border-radius:6px;margin:16px 0;">
                  <strong>Inasistencias registradas este mes:</strong>
                  <span style="font-size:1.5rem;font-weight:900;color:#c0392b;margin-left:8px;">%d</span>
                </div>""".formatted(faltas) : "";
        String bloqueMensaje = mensaje == null ? "" : """
                <div style="background:#f8f9fa;border-left:4px solid %s;padding:14px 16px;border-radius:6px;margin:16px 0;">
                  <strong>Observación:</strong><br>%s
                </div>""".formatted(color, e(mensaje).replace("\n", "<br>"));
        return """
                <div style="font-family:Arial,sans-serif;color:#333;max-width:640px;margin:0 auto;border:1px solid #ddd;border-radius:8px;overflow:hidden;background:#fff;">
                  <div style="background:%1$s;padding:24px;text-align:center;">
                    <h2 style="color:#fff;margin:0;font-size:1.4rem;">SENA - %2$s</h2>
                    <p style="color:rgba(255,255,255,0.9);margin:6px 0 0;font-size:0.95rem;">%3$s</p>
                  </div>
                  <div style="padding:28px;">
                    <div style="display:inline-block;border:1px solid %1$s;color:%1$s;padding:6px 14px;border-radius:20px;font-size:0.85rem;font-weight:700;margin-bottom:16px;">%4$s</div>
                    <h3 style="margin-top:0;">Estimado/a %5$s,</h3>
                    <p>Le informamos que ha sido registrado(a) un <strong>%6$s</strong> en el Sistema de Acceso SENA el día <strong>%7$s</strong>.</p>
                    %8$s
                    %9$s
                    <p>Le solicitamos atender el presente comunicado y tomar las medidas correspondientes a la brevedad posible.</p>
                    <p>Si considera que este comunicado es un error, por favor comuníquese directamente con:</p>
                    <div style="background:#f1f1f1;padding:12px 16px;border-radius:6px;margin:16px 0;">
                      <strong>%10$s</strong> — %11$s<br>
                      <span style="color:#555;font-size:0.9rem;">%2$s - SENA</span>
                    </div>
                    <div style="background:#f9f9f9;border-left:4px solid #39A900;padding:12px;margin-top:20px;font-size:13px;color:#555;">
                      <strong>Política de privacidad:</strong> Este comunicado es de uso exclusivo del Sistema de Acceso SENA y se emite conforme a las políticas institucionales de convivencia y reglamento interno.
                    </div>
                  </div>
                  <div style="background:#f4f4f4;padding:16px;text-align:center;font-size:12px;color:#777;">
                    <p style="margin:0;">Correo generado automáticamente — No responda a este mensaje.</p>
                  </div>
                </div>""".formatted(color, e(centro), e(regional),
                tipoMayusculas, e(destino.getNombre()), t, fecha.format(FECHA), bloqueFaltas,
                bloqueMensaje, e(remitente.getNombre()),
                e(remitente.getCargo() != null ? remitente.getCargo() : "Instructor"));
    }
}
