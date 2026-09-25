package co.sena.adso.porteria.service;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Si un correo no llega, la persona no activa su cuenta: se prefiere fiabilidad y un log claro
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    public enum Resultado { ENTREGADO, FALLO_PASAJERO, FALLO_DEFINITIVO }

    public static final Set<String> MODOS_CIFRADO = Set.of("ssl", "starttls", "ninguno");
    public static final int MAX_REINTENTOS = 2;
    private static final int TIEMPO_ESPERA_MS = 20_000;
    private static final int MAX_FALLOS_RECORDADOS = 50;

    public record Config(String servidor, int puerto, String usuario, String clave, String remitente,
                         String nombreRemitente, String cifrado, String modo, boolean respaldoRele) {

        // El cifrado se valida aquí: un valor mal escrito nunca debe dejar la conexión en claro
        public static Config de(String servidor, int puerto, String usuario, String clave, String remitente,
                                String nombreRemitente, String cifrado, String modo, boolean respaldoRele) {
            return new Config(texto(servidor), puerto, texto(usuario), texto(clave),
                    texto(remitente).isEmpty() ? texto(usuario) : texto(remitente), texto(nombreRemitente),
                    modoCifrado(puerto, cifrado), texto(modo).isEmpty() ? "rele" : texto(modo).toLowerCase(), respaldoRele);
        }
    }

    public record Pendiente(MimeMessage mensaje, String destino, int intentos) {
    }

    // Un canal SMTP; separado para poder simularlo sin abrir sockets
    public interface Conexion {
        void starttls() throws MessagingException;

        void login(String usuario, String clave) throws MessagingException;

        void enviar(MimeMessage mensaje) throws MessagingException;

        void cerrar();
    }

    public interface Fabrica {
        Conexion abrir(String servidor, int puerto, boolean cifradoDesdeElInicio) throws MessagingException;
    }

    public interface EntregaDirecta {
        boolean entregar(MimeMessage mensaje, String destino);
    }

    public static class NoSoportadoException extends MessagingException {
        public NoSoportadoException(String mensaje) {
            super(mensaje);
        }
    }

    private final Config config;
    private final Fabrica fabrica;
    private final EntregaDirecta entregaDirecta;
    private final boolean iniciarEnviador;
    private final BlockingQueue<Pendiente> cola = new LinkedBlockingQueue<>();
    private final AtomicInteger reintentosEnEspera = new AtomicInteger();
    private final AtomicInteger enVuelo = new AtomicInteger();
    private final Deque<Map<String, String>> fallos = new ArrayDeque<>();
    private final ScheduledExecutorService temporizador = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread hilo = new Thread(r, "reintento-correo");
        hilo.setDaemon(true);
        return hilo;
    });
    private long esperaBaseMs = 5_000;
    private long pausaEntreCorreosMs = 1_200;
    private Thread enviador;
    private boolean avisoDirectoDado;

    @Autowired
    public CorreoService(@Value("${app.correo.servidor:}") String servidor,
                         @Value("${app.correo.puerto:587}") int puerto,
                         @Value("${app.correo.usuario:}") String usuario,
                         @Value("${app.correo.clave:}") String clave,
                         @Value("${app.correo.remitente:}") String remitente,
                         @Value("${app.correo.nombre-remitente:Sistema de Acceso SENA}") String nombreRemitente,
                         @Value("${app.correo.cifrado:}") String cifrado,
                         @Value("${app.correo.modo:rele}") String modo,
                         @Value("${app.correo.respaldo-rele:true}") boolean respaldoRele) {
        this(Config.de(servidor, puerto, usuario, clave, remitente, nombreRemitente, cifrado, modo, respaldoRele),
                CorreoService::conexionReal, CorreoService::entregarDirecto, true);
    }

    public CorreoService(Config config, Fabrica fabrica, EntregaDirecta entregaDirecta, boolean iniciarEnviador) {
        this.config = config;
        this.fabrica = fabrica;
        this.entregaDirecta = entregaDirecta;
        this.iniciarEnviador = iniciarEnviador;
    }

    public boolean configurado() {
        return !config.servidor().isEmpty() || "directo".equals(config.modo());
    }

    // true solo dice que quedó en cola: el resultado real queda en el log y en fallosRecientes
    public boolean enviar(String para, String asunto, String html) {
        if (!configurado()) {
            log.warn("SMTP_HOST no configurado: no se envió el correo a {}", ofuscar(para));
            return false;
        }
        String destino = sinSaltos(para).trim();
        if (config.remitente().isEmpty() || !direccionValida(destino)) {
            log.warn("Remitente o destinatario no válido: no se envió el correo");
            return false;
        }
        try {
            MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
            mensaje.setFrom(config.nombreRemitente().isEmpty() ? new InternetAddress(config.remitente())
                    : new InternetAddress(config.remitente(), sinSaltos(config.nombreRemitente()), "UTF-8"));
            mensaje.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(destino));
            mensaje.setSubject(sinSaltos(asunto).replace('\t', ' '), "UTF-8");
            mensaje.setContent(html, "text/html; charset=UTF-8");
            if ("directo".equals(config.modo())) {
                avisarRiesgoDirecto();
            }
            asegurarEnviador();
            cola.add(new Pendiente(mensaje, destino, 0));
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("No se pudo armar el correo: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public int pendientes() {
        return cola.size() + reintentosEnEspera.get() + enVuelo.get();
    }

    public List<Map<String, String>> fallosRecientes() {
        synchronized (fallos) {
            return new ArrayList<>(fallos);
        }
    }

    // Deja el correo reconocible en el log sin escribirlo entero (Ley 1581)
    public static String ofuscar(String direccion) {
        if (direccion == null || !direccion.contains("@")) {
            return "(sin destinatario)";
        }
        int arroba = direccion.lastIndexOf('@');
        String usuario = direccion.substring(0, arroba);
        String visible = usuario.length() > 3 ? usuario.substring(0, 2) : usuario.substring(0, Math.min(1, usuario.length()));
        return visible + "***@" + direccion.substring(arroba + 1);
    }

    // 465 cifra desde el primer byte, 25 solo sirve contra un relé local y el resto usa STARTTLS
    public static String modoCifrado(int puerto, String configurado) {
        if (configurado != null && !configurado.isBlank()) {
            String elegido = configurado.trim().toLowerCase();
            if (MODOS_CIFRADO.contains(elegido)) {
                return elegido;
            }
            log.error("SMTP_CIFRADO={} no es válido (ssl, starttls o ninguno): se deduce del puerto", configurado);
        }
        if (puerto == 465) {
            return "ssl";
        }
        return puerto == 25 ? "ninguno" : "starttls";
    }

    private Resultado entregar(MimeMessage mensaje, String destino) {
        if ("directo".equals(config.modo())) {
            if (entregaDirecta.entregar(mensaje, destino)) {
                return Resultado.ENTREGADO;
            }
            if (!config.respaldoRele() || config.servidor().isEmpty()) {
                return Resultado.FALLO_PASAJERO;
            }
            log.warn("Entrega directa fallida, se intenta por el relé");
        }
        Conexion conexion = null;
        try {
            conexion = fabrica.abrir(config.servidor(), config.puerto(), "ssl".equals(config.cifrado()));
            if ("starttls".equals(config.cifrado())) {
                conexion.starttls();
            }
            if (!config.usuario().isEmpty() && !config.clave().isEmpty()) {
                conexion.login(config.usuario(), config.clave());
            }
            conexion.enviar(mensaje);
            log.info("Correo entregado al servidor SMTP para {}", ofuscar(destino));
            return Resultado.ENTREGADO;
        } catch (AuthenticationFailedException e) {
            log.error("SMTP rechazó las credenciales de {}. Si es Gmail, se usa una contraseña de aplicación",
                    ofuscar(config.usuario()));
            return Resultado.FALLO_DEFINITIVO;
        } catch (SMTPAddressFailedException e) {
            log.error("El servidor rechazó al destinatario {}: se descarta", ofuscar(destino));
            return Resultado.FALLO_DEFINITIVO;
        } catch (SMTPSenderFailedException e) {
            log.error("El servidor rechazó al remitente {}: insistir no lo arregla", ofuscar(config.remitente()));
            return Resultado.FALLO_DEFINITIVO;
        } catch (SMTPSendFailedException e) {
            // 5xx es un rechazo permanente; 4xx es "ahora no, prueba luego"
            if (e.getReturnCode() >= 500) {
                log.error("Rechazo permanente ({}) enviando a {}: se descarta", e.getReturnCode(), ofuscar(destino));
                return Resultado.FALLO_DEFINITIVO;
            }
            log.error("Rechazo temporal ({}) enviando a {}", e.getReturnCode(), ofuscar(destino));
            return Resultado.FALLO_PASAJERO;
        } catch (NoSoportadoException e) {
            log.error("El servidor no admite lo que pide la configuración ({}:{}, cifrado={})",
                    config.servidor(), config.puerto(), config.cifrado());
            return Resultado.FALLO_DEFINITIVO;
        } catch (SendFailedException e) {
            if (e.getInvalidAddresses() != null && e.getInvalidAddresses().length > 0) {
                log.error("Dirección rechazada enviando a {}: se descarta", ofuscar(destino));
                return Resultado.FALLO_DEFINITIVO;
            }
            log.error("Fallo enviando a {} ({})", ofuscar(destino), e.getClass().getSimpleName());
            return Resultado.FALLO_PASAJERO;
        } catch (MessagingException | RuntimeException e) {
            log.error("Error enviando correo a {} ({}:{}, cifrado={}): {}", ofuscar(destino), config.servidor(),
                    config.puerto(), config.cifrado(), e.getClass().getSimpleName());
            return Resultado.FALLO_PASAJERO;
        } finally {
            if (conexion != null) {
                conexion.cerrar();
            }
        }
    }

    private Resultado procesarUno(Pendiente pendiente) {
        Resultado resultado = entregar(pendiente.mensaje(), pendiente.destino());
        if (resultado == Resultado.FALLO_DEFINITIVO) {
            log.error("Correo a {} descartado sin reintentos: el fallo no se arregla insistiendo",
                    ofuscar(pendiente.destino()));
            registrarFallo(pendiente, "Descartado sin reintentos.");
        } else if (resultado == Resultado.FALLO_PASAJERO && pendiente.intentos() < MAX_REINTENTOS) {
            programarReintento(pendiente);
        } else if (resultado == Resultado.FALLO_PASAJERO) {
            log.error("Correo a {} abandonado tras {} intentos", ofuscar(pendiente.destino()), MAX_REINTENTOS + 1);
            registrarFallo(pendiente, "Abandonado tras " + (MAX_REINTENTOS + 1) + " intentos.");
        }
        return resultado;
    }

    // La espera va en un temporizador aparte: un correo problemático no frena a los que vienen detrás
    private ScheduledFuture<?> programarReintento(Pendiente pendiente) {
        long espera = esperaBaseMs * (long) Math.pow(4, pendiente.intentos());
        log.info("Reintentando el correo a {} en {} ms (intento {} de {})", ofuscar(pendiente.destino()), espera,
                pendiente.intentos() + 2, MAX_REINTENTOS + 1);
        reintentosEnEspera.incrementAndGet();
        return temporizador.schedule(() -> {
            try {
                asegurarEnviador();
                cola.add(new Pendiente(pendiente.mensaje(), pendiente.destino(), pendiente.intentos() + 1));
            } finally {
                liberarReintento();
            }
        }, espera, TimeUnit.MILLISECONDS);
    }

    // Nunca baja de cero: un valor negativo haría creer que no queda nada por enviar
    private void liberarReintento() {
        reintentosEnEspera.getAndUpdate(n -> {
            if (n <= 0) {
                log.error("El contador de reintentos iba a quedar negativo; se deja en 0");
                return 0;
            }
            return n - 1;
        });
    }

    private void registrarFallo(Pendiente pendiente, String motivo) {
        String asunto;
        try {
            asunto = pendiente.mensaje().getSubject();
        } catch (MessagingException | RuntimeException e) {
            asunto = "";
        }
        synchronized (fallos) {
            fallos.addFirst(Map.of("destinatario", ofuscar(pendiente.destino()), "asunto", asunto == null ? "" : asunto,
                    "motivo", motivo, "momento", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            while (fallos.size() > MAX_FALLOS_RECORDADOS) {
                fallos.removeLast();
            }
        }
    }

    private synchronized void asegurarEnviador() {
        if (!iniciarEnviador || (enviador != null && enviador.isAlive())) {
            return;
        }
        enviador = new Thread(this::procesarCola, "enviador-correo");
        enviador.setDaemon(true);
        enviador.start();
    }

    // Un solo hilo: el servidor de correo ve un flujo ordenado y no una avalancha que dispare bloqueos
    private void procesarCola() {
        while (true) {
            Pendiente pendiente;
            try {
                pendiente = cola.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            enVuelo.incrementAndGet();
            try {
                procesarUno(pendiente);
            } catch (RuntimeException e) {
                log.error("Fallo inesperado enviando un correo de la cola: {}", e.getClass().getSimpleName());
            } finally {
                enVuelo.decrementAndGet();
            }
            try {
                Thread.sleep(pausaEntreCorreosMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void avisarRiesgoDirecto() {
        if (avisoDirectoDado) {
            return;
        }
        avisoDirectoDado = true;
        log.warn("SMTP_MODO=directo activo: sin SPF, DKIM, DMARC y DNS inverso los correos terminan en spam "
                + "y nadie podrá activar su cuenta");
    }

    private static boolean direccionValida(String destino) {
        if (!destino.contains("@")) {
            return false;
        }
        try {
            new InternetAddress(destino, true).validate();
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

    private static String sinSaltos(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\r\\n]", "");
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static Conexion conexionReal(String servidor, int puerto, boolean ssl) {
        return new Conexion() {
            private boolean starttls;
            private Transport transporte;

            private void conectar(String usuario, String clave) throws MessagingException {
                Properties props = new Properties();
                props.put("mail.smtp.connectiontimeout", TIEMPO_ESPERA_MS);
                props.put("mail.smtp.timeout", TIEMPO_ESPERA_MS);
                props.put("mail.smtp.writetimeout", TIEMPO_ESPERA_MS);
                props.put("mail.smtp.ssl.enable", ssl);
                props.put("mail.smtp.starttls.enable", starttls);
                props.put("mail.smtp.starttls.required", starttls);
                props.put("mail.smtp.auth", usuario != null);
                transporte = Session.getInstance(props).getTransport("smtp");
                try {
                    transporte.connect(servidor, puerto, usuario, clave);
                } catch (MessagingException e) {
                    if (e.getMessage() != null && e.getMessage().contains("STARTTLS")) {
                        throw new NoSoportadoException("El servidor no admite STARTTLS");
                    }
                    throw e;
                }
            }

            @Override
            public void starttls() {
                starttls = true;
            }

            @Override
            public void login(String usuario, String clave) throws MessagingException {
                conectar(usuario, clave);
            }

            @Override
            public void enviar(MimeMessage mensaje) throws MessagingException {
                if (transporte == null) {
                    conectar(null, null);
                }
                mensaje.saveChanges();
                transporte.sendMessage(mensaje, mensaje.getAllRecipients());
            }

            @Override
            public void cerrar() {
                try {
                    if (transporte != null) {
                        transporte.close();
                    }
                } catch (MessagingException ignorado) {
                    // Cerrar un canal que ya falló no cambia el resultado
                }
            }
        };
    }

    // Modo directo: habla con el MX del destinatario por el puerto 25, sin relé
    private static boolean entregarDirecto(MimeMessage mensaje, String destino) {
        String dominio = destino.substring(destino.lastIndexOf('@') + 1);
        for (String servidor : servidoresDe(dominio)) {
            Conexion conexion = conexionReal(servidor, 25, false);
            try {
                conexion.starttls();
                conexion.enviar(mensaje);
                log.info("Correo entregado directamente a {} vía {}", ofuscar(destino), servidor);
                return true;
            } catch (MessagingException | RuntimeException e) {
                log.warn("Falló la entrega directa vía {}: {}", servidor, e.getClass().getSimpleName());
            } finally {
                conexion.cerrar();
            }
        }
        log.error("No se pudo entregar directamente a {}: revisa que el puerto 25 de salida esté abierto", ofuscar(destino));
        return false;
    }

    private static List<String> servidoresDe(String dominio) {
        try {
            Attribute mx = new InitialDirContext(new java.util.Hashtable<>(Map.of(
                    "java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory")))
                    .getAttributes("dns:/" + dominio, new String[] {"MX"}).get("MX");
            if (mx == null) {
                return List.of(dominio);
            }
            List<String[]> registros = new ArrayList<>();
            for (int i = 0; i < mx.size(); i++) {
                registros.add(mx.get(i).toString().split(" "));
            }
            registros.sort((a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
            return registros.stream().map(r -> r[1].replaceAll("\\.$", "")).toList();
        } catch (Exception e) {
            log.warn("No se pudieron resolver los MX de {}", dominio);
            return List.of(dominio);
        }
    }
}
