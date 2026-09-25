package co.sena.adso.porteria.modulos;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.sena.adso.porteria.service.CorreoService;
import co.sena.adso.porteria.service.CorreoService.Config;
import co.sena.adso.porteria.service.CorreoService.Conexion;
import co.sena.adso.porteria.service.CorreoService.Pendiente;
import co.sena.adso.porteria.service.CorreoService.Resultado;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

// Portería 2: tests/modulos/test_email.py
// El servidor SMTP se simula: no se abre ningún socket y se anota el cifrado del canal en cada orden
class EmailTest {

    private static final String DESTINO = "aprendiz.identificable@sena.edu.co";
    private static final String REMITENTE = "remitente.identificable@sena.edu.co";

    // Lo que ve el servidor simulado, conexión por conexión
    static class RegistroSmtp {
        final List<ConexionSimulada> conexiones = new ArrayList<>();
        final List<String> cifradoAlAutenticar = new ArrayList<>();
        final List<String> cifradoAlEnviar = new ArrayList<>();

        ConexionSimulada ultima() {
            return conexiones.get(conexiones.size() - 1);
        }
    }

    static class ConexionSimulada implements Conexion {
        final RegistroSmtp registro;
        final List<String> ordenes = new ArrayList<>();
        final List<MimeMessage> mensajes = new ArrayList<>();
        final MessagingException falloLogin;
        final MessagingException falloEnvio;
        String cifrado;

        ConexionSimulada(RegistroSmtp registro, String cifradoInicial, MessagingException falloLogin, MessagingException falloEnvio) {
            this.registro = registro;
            this.cifrado = cifradoInicial;
            this.falloLogin = falloLogin;
            this.falloEnvio = falloEnvio;
            registro.conexiones.add(this);
        }

        @Override
        public void starttls() throws MessagingException {
            if ("ssl".equals(cifrado)) {
                throw new CorreoService.NoSoportadoException("STARTTLS sobre un canal ya cifrado");
            }
            ordenes.add("starttls");
            cifrado = "starttls";
        }

        @Override
        public void login(String usuario, String clave) throws MessagingException {
            ordenes.add("login");
            registro.cifradoAlAutenticar.add(cifrado);
            if (falloLogin != null) {
                throw falloLogin;
            }
        }

        @Override
        public void enviar(MimeMessage mensaje) throws MessagingException {
            ordenes.add("send_message");
            registro.cifradoAlEnviar.add(cifrado);
            if (falloEnvio != null) {
                throw falloEnvio;
            }
            mensajes.add(mensaje);
        }

        @Override
        public void cerrar() {
            ordenes.add("quit");
        }
    }

    private final RegistroSmtp registro = new RegistroSmtp();
    private final ListAppender<ILoggingEvent> log = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(CorreoService.class);
    private Level nivelAnterior;
    private final List<CorreoService> creados = new ArrayList<>();

    @BeforeEach
    void capturarLog() {
        nivelAnterior = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        log.start();
        logger.addAppender(log);
    }

    // Ningún temporizador de una prueba debe dispararse en la siguiente
    @AfterEach
    void limpiar() {
        logger.detachAppender(log);
        logger.setLevel(nivelAnterior);
        creados.forEach(s -> ((ScheduledExecutorService) ReflectionTestUtils.getField(s, "temporizador")).shutdownNow());
    }

    private String textoDelLog() {
        return log.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }

    private static Config config(String cifrado, int puerto, String modo, boolean respaldoRele) {
        return new Config("smtp.sena.edu.co", puerto, REMITENTE, "secreta", REMITENTE, "Sistema de Acceso SENA",
                cifrado, modo, respaldoRele);
    }

    private CorreoService servicio(Config config, MessagingException falloConexion, MessagingException falloLogin,
                                   MessagingException falloEnvio, CorreoService.EntregaDirecta directa, boolean iniciar) {
        CorreoService servicio = new CorreoService(config, (servidor, puerto, ssl) -> {
            if (falloConexion != null) {
                throw falloConexion;
            }
            return new ConexionSimulada(registro, ssl ? "ssl" : "ninguno", falloLogin, falloEnvio);
        }, directa, iniciar);
        creados.add(servicio);
        return servicio;
    }

    private CorreoService servicio(MessagingException falloLogin, MessagingException falloEnvio) {
        return servicio(config("starttls", 587, "rele", true), null, falloLogin, falloEnvio, (m, d) -> false, false);
    }

    private static MimeMessage mensaje() throws MessagingException {
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        mensaje.setText("hola");
        return mensaje;
    }

    private static Resultado entregar(CorreoService servicio) throws MessagingException {
        return ReflectionTestUtils.invokeMethod(servicio, "entregar", mensaje(), DESTINO);
    }

    private static void procesarUno(CorreoService servicio, int intentos) throws MessagingException {
        ReflectionTestUtils.invokeMethod(servicio, "procesarUno", new Pendiente(mensaje(), DESTINO, intentos));
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueue<Pendiente> cola(CorreoService servicio) {
        return (BlockingQueue<Pendiente>) ReflectionTestUtils.getField(servicio, "cola");
    }

    private static AtomicInteger reintentosEnEspera(CorreoService servicio) {
        return (AtomicInteger) ReflectionTestUtils.getField(servicio, "reintentosEnEspera");
    }

    private static CorreoService siempre(CorreoService servicio, long esperaBaseMs) {
        ReflectionTestUtils.setField(servicio, "esperaBaseMs", esperaBaseMs);
        return servicio;
    }

    // 1. Fallos definitivos frente a fallos pasajeros

    @Test
    void envioCorrectoDevuelveEntregado() throws Exception {
        assertThat(entregar(servicio(null, null))).isEqualTo(Resultado.ENTREGADO);
        assertThat(registro.ultima().mensajes).hasSize(1);
    }

    // Credenciales mal puestas no mejoran reintentando: con 300 aprendices eso bloquea la cuenta del remitente
    @Test
    void credencialesRechazadasEsFalloDefinitivo() throws Exception {
        assertThat(entregar(servicio(new AuthenticationFailedException("535 Bad credentials"), null)))
                .isEqualTo(Resultado.FALLO_DEFINITIVO);
    }

    @Test
    void destinatarioInexistenteEsFalloDefinitivo() throws Exception {
        SMTPAddressFailedException fallo = new SMTPAddressFailedException(new InternetAddress(DESTINO), "RCPT TO", 550, "No such user");
        assertThat(entregar(servicio(null, fallo))).isEqualTo(Resultado.FALLO_DEFINITIVO);
    }

    @Test
    void remitenteRechazadoEsFalloDefinitivo() throws Exception {
        SMTPSenderFailedException fallo = new SMTPSenderFailedException(new InternetAddress(REMITENTE), "MAIL FROM", 553,
                "Sender not allowed");
        assertThat(entregar(servicio(null, fallo))).isEqualTo(Resultado.FALLO_DEFINITIVO);
    }

    @Test
    void rechazo5xxEsFalloDefinitivo() throws Exception {
        SMTPSendFailedException fallo = new SMTPSendFailedException("DATA", 552, "Message too large", null, null, null, null);
        assertThat(entregar(servicio(null, fallo))).isEqualTo(Resultado.FALLO_DEFINITIVO);
    }

    // 4xx es "ahora no, prueba luego": eso sí merece reintentarse
    @Test
    void rechazo4xxEsFalloPasajero() throws Exception {
        SMTPSendFailedException fallo = new SMTPSendFailedException("DATA", 451, "Try again later", null, null, null, null);
        assertThat(entregar(servicio(null, fallo))).isEqualTo(Resultado.FALLO_PASAJERO);
    }

    @Test
    void corteDeRedEsFalloPasajero() throws Exception {
        CorreoService servicio = servicio(config("starttls", 587, "rele", true),
                new MessagingException("conexión rechazada", new ConnectException()), null, null, (m, d) -> false, false);
        assertThat(entregar(servicio)).isEqualTo(Resultado.FALLO_PASAJERO);
    }

    @Test
    void servidorQueCuelgaEsFalloPasajero() throws Exception {
        assertThat(entregar(servicio(null, new MessagingException("[EOF]")))).isEqualTo(Resultado.FALLO_PASAJERO);
    }

    // 2. La cola: qué se reintenta y qué no

    @Test
    void falloDefinitivoNoSeReintenta() throws Exception {
        CorreoService servicio = servicio(new AuthenticationFailedException("535"), null);
        procesarUno(servicio, 0);
        Thread.sleep(200);
        assertThat(servicio.pendientes()).as("un fallo definitivo se reencoló").isZero();
        assertThat(textoDelLog()).contains("descartado sin reintentos");
    }

    @Test
    void falloPasajeroSiSeReintenta() throws Exception {
        CorreoService servicio = siempre(servicio(null, new MessagingException("[EOF]")), 0);
        procesarUno(servicio, 0);
        Pendiente reencolado = cola(servicio).poll(5, TimeUnit.SECONDS);
        assertThat(reencolado).isNotNull();
        assertThat(reencolado.destino()).isEqualTo(DESTINO);
        assertThat(reencolado.intentos()).as("el contador de intentos no avanzó").isEqualTo(1);
    }

    @Test
    void noSeReintentaIndefinidamente() throws Exception {
        CorreoService servicio = siempre(servicio(null, new MessagingException("[EOF]")), 0);
        procesarUno(servicio, CorreoService.MAX_REINTENTOS);
        Thread.sleep(200);
        assertThat(servicio.pendientes()).isZero();
        assertThat(textoDelLog()).contains("abandonado");
    }

    // Si la espera ocurriera en el hilo enviador, un correo problemático frenaría hasta 25 s a los de atrás
    @Test
    void laEsperaDelReintentoNoBloqueaAlResto() throws Exception {
        CorreoService servicio = siempre(servicio(null, new MessagingException("[EOF]")), 30_000);
        long inicio = System.nanoTime();
        procesarUno(servicio, 0);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio)).as("el enviador se quedó esperando").isLessThan(1000);
        // Sigue contando como pendiente aunque no esté en la cola
        assertThat(servicio.pendientes()).isEqualTo(1);
    }

    // Un reintento pendiente no puede impedir que el proceso se apague
    @Test
    void elTemporizadorDelReintentoEsDemonio() throws Exception {
        ScheduledExecutorService temporizador = (ScheduledExecutorService) ReflectionTestUtils.getField(
                servicio(null, null), "temporizador");
        assertThat(temporizador.submit(() -> Thread.currentThread().isDaemon()).get()).isTrue();
    }

    // Reproduce el error real "Quedaron -1 correos sin enviar al apagar"
    @Test
    void correosPendientesNuncaEsNegativoPorLaRutaDelReintento() throws Exception {
        CorreoService servicio = siempre(servicio(null, new MessagingException("[EOF]")), 0);
        assertThat(servicio.pendientes()).isZero();

        procesarUno(servicio, 0);
        long fin = System.currentTimeMillis() + 5000;
        while (cola(servicio).isEmpty() && System.currentTimeMillis() < fin) {
            Thread.sleep(50);
        }
        assertThat(cola(servicio)).as("el reintento nunca volvió a la cola").isNotEmpty();
        assertThat(reintentosEnEspera(servicio).get()).isZero();
        assertThat(servicio.pendientes()).isEqualTo(1);

        cola(servicio).poll();
        siempre(servicio, 30_000);
        ScheduledFuture<?> reintento = ReflectionTestUtils.invokeMethod(servicio, "programarReintento",
                new Pendiente(mensaje(), DESTINO, 0));
        reintento.cancel(false);
        assertThat(reintentosEnEspera(servicio).get()).isEqualTo(1);

        // Dos decrementos para un solo reintento, como un temporizador fantasma que dispara tarde
        ReflectionTestUtils.invokeMethod(servicio, "liberarReintento");
        ReflectionTestUtils.invokeMethod(servicio, "liberarReintento");
        assertThat(reintentosEnEspera(servicio).get()).as("un decremento de más dejó el contador negativo").isZero();
        assertThat(servicio.pendientes()).isGreaterThanOrEqualTo(0);
    }

    // Un correo que ya salió de la cola pero sigue hablando con el SMTP cuenta como pendiente
    @Test
    void correosPendientesCuentaElCorreoEnVuelo() throws Exception {
        List<Integer> duranteElEnvio = new ArrayList<>();
        CorreoService[] servicio = new CorreoService[1];
        servicio[0] = new CorreoService(config("starttls", 587, "rele", true), (s, p, ssl) -> new ConexionSimulada(registro, "ninguno", null, null) {
            @Override
            public void enviar(MimeMessage mensaje) {
                duranteElEnvio.add(servicio[0].pendientes());
            }
        }, (m, d) -> false, true);
        creados.add(servicio[0]);
        ReflectionTestUtils.setField(servicio[0], "pausaEntreCorreosMs", 0L);

        assertThat(servicio[0].enviar(DESTINO, "Código", "<p>hola</p>")).isTrue();
        long fin = System.currentTimeMillis() + 5000;
        while (duranteElEnvio.isEmpty() && System.currentTimeMillis() < fin) {
            Thread.sleep(20);
        }
        assertThat(duranteElEnvio).containsExactly(1);
        while (servicio[0].pendientes() != 0 && System.currentTimeMillis() < fin) {
            Thread.sleep(20);
        }
        assertThat(servicio[0].pendientes()).isZero();
    }

    // 3. Cifrado

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"465, ssl", "587, starttls", "25, ninguno", "2525, starttls"})
    void cadaPuertoEligeSuCifrado(int puerto, String esperado) {
        assertThat(CorreoService.modoCifrado(puerto, null)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"ssl", "STARTTLS", " ninguno "})
    void unValorValidoDeMailCifradoSeRespeta(String valor) {
        assertThat(CorreoService.modoCifrado(587, valor)).isEqualTo(valor.trim().toLowerCase());
    }

    // "tls" colado tal cual abriría la conexión en claro y la contraseña SMTP saldría en texto plano
    @ParameterizedTest(name = "{0} en {1}")
    @CsvSource({"tls, 465", "tls, 587", "tls, 25", "TLS, 465", "TLS, 587", "TLS, 25", "si, 465", "si, 587", "si, 25",
            "true, 465", "true, 587", "true, 25", "ssl/tls, 465", "ssl/tls, 587", "ssl/tls, 25", "1, 465", "1, 587", "1, 25"})
    void mailCifradoInvalidoNoDesactivaElCifrado(String valor, int puerto) {
        String resultado = CorreoService.modoCifrado(puerto, valor);
        assertThat(CorreoService.MODOS_CIFRADO).contains(resultado);
        assertThat(resultado).as("no se dedujo del puerto").isEqualTo(CorreoService.modoCifrado(puerto, null));
        assertThat(textoDelLog()).as("el valor inválido no quedó registrado").contains("SMTP_CIFRADO");
    }

    @Test
    void puerto465AbreElCanalYaCifrado() throws Exception {
        CorreoService servicio = servicio(config("ssl", 465, "rele", true), null, null, null, (m, d) -> false, false);
        assertThat(entregar(servicio)).isEqualTo(Resultado.ENTREGADO);
        assertThat(registro.cifradoAlAutenticar).containsExactly("ssl");
        assertThat(registro.ultima().ordenes).doesNotContain("starttls");
    }

    @Test
    void puerto587CifraAntesDeAutenticar() throws Exception {
        assertThat(entregar(servicio(null, null))).isEqualTo(Resultado.ENTREGADO);
        List<String> ordenes = registro.ultima().ordenes;
        assertThat(ordenes.indexOf("starttls")).isLessThan(ordenes.indexOf("login"));
        assertThat(registro.cifradoAlAutenticar).containsExactly("starttls");
    }

    // De extremo a extremo: SMTP_CIFRADO=tls en el 587 no puede terminar en un login sin cifrar
    @Test
    void unMailCifradoInvalidoNoMandaLaClaveEnClaro() throws Exception {
        Config cfg = Config.de("smtp.sena.edu.co", 587, REMITENTE, "secreta", "", "", "tls", "rele", true);
        assertThat(cfg.cifrado()).isEqualTo("starttls");
        assertThat(entregar(servicio(cfg, null, null, null, (m, d) -> false, false))).isEqualTo(Resultado.ENTREGADO);
        assertThat(registro.cifradoAlAutenticar).as("la contraseña SMTP salió por un canal sin cifrar").doesNotContain("ninguno");
        assertThat(registro.cifradoAlEnviar).doesNotContain("ninguno");
    }

    // 4. Minimización de datos personales (Ley 1581 de 2012)

    @Test
    void lasCredencialesRechazadasNoEscribenElRemitenteEntero() throws Exception {
        entregar(servicio(new AuthenticationFailedException("535"), null));
        assertThat(textoDelLog()).doesNotContain(REMITENTE).contains("re***@sena.edu.co");
    }

    @ParameterizedTest(name = "[{index}] fallo de login: {0}")
    @ValueSource(booleans = {false, true})
    void ningunaDireccionCompletaApareceEnElRegistro(boolean falloLogin) throws Exception {
        entregar(servicio(falloLogin ? new AuthenticationFailedException("535") : null, null));
        assertThat(textoDelLog()).doesNotContain(DESTINO).doesNotContain(REMITENTE);
    }

    @Test
    void elDestinatarioRechazadoTampocoSeEscribeEntero() throws Exception {
        procesarUno(servicio(null, new SMTPAddressFailedException(new InternetAddress(DESTINO), "RCPT TO", 550, "No such user")), 0);
        assertThat(textoDelLog()).doesNotContain(DESTINO).contains("ap***@sena.edu.co");
    }

    @Test
    void ofuscarNoRevelaElUsuarioCompleto() {
        assertThat(CorreoService.ofuscar("juan.perez@sena.edu.co")).isEqualTo("ju***@sena.edu.co");
        assertThat(CorreoService.ofuscar("ana@sena.edu.co")).isEqualTo("a***@sena.edu.co");
        assertThat(CorreoService.ofuscar(null)).isEqualTo("(sin destinatario)");
        assertThat(CorreoService.ofuscar("sin-arroba")).isEqualTo("(sin destinatario)");
    }

    // 5. El hilo enviador y el comportamiento externo

    // Si el hilo muere, la cola se queda parada y nadie se entera
    @Test
    void elHiloEnviadorSobreviveAUnElementoCorrupto() throws Exception {
        CorreoService servicio = servicio(config("starttls", 587, "rele", true), null, null, null, (m, d) -> false, true);
        ReflectionTestUtils.setField(servicio, "pausaEntreCorreosMs", 0L);
        ReflectionTestUtils.invokeMethod(servicio, "asegurarEnviador");
        Thread hilo = (Thread) ReflectionTestUtils.getField(servicio, "enviador");
        assertThat(hilo.isAlive()).isTrue();

        cola(servicio).add(new Pendiente(null, null, 0));
        long fin = System.currentTimeMillis() + 5000;
        while (!cola(servicio).isEmpty() && System.currentTimeMillis() < fin) {
            Thread.sleep(50);
        }
        assertThat(cola(servicio)).as("el hilo no llegó a consumir el elemento").isEmpty();
        assertThat(hilo.isAlive()).as("el hilo enviador murió y dejó la cola parada").isTrue();
    }

    // La petición web no espera al servidor de correo
    @Test
    void enviarCorreoEncolaYVuelvePronto() {
        CorreoService servicio = servicio(null, null);
        long inicio = System.nanoTime();
        assertThat(servicio.enviar(DESTINO, "Código", "<p>hola</p>")).isTrue();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio)).isLessThan(1000);
        Pendiente encolado = cola(servicio).poll();
        assertThat(encolado.destino()).isEqualTo(DESTINO);
        assertThat(encolado.intentos()).isZero();
    }

    @Test
    void enviarCorreoRechazaUnDestinatarioInvalido() {
        CorreoService servicio = servicio(null, null);
        assertThat(servicio.enviar("esto-no-es-un-correo", "x", "y")).isFalse();
        assertThat(cola(servicio)).isEmpty();
    }

    // Se conserva a petición expresa, aunque no sea el modo recomendado
    @Test
    void elModoDirectoSeConserva() throws Exception {
        List<String> llamadas = new ArrayList<>();
        CorreoService servicio = servicio(config("starttls", 587, "directo", true), null, null, null, (m, d) -> {
            llamadas.add(d);
            return true;
        }, false);
        assertThat(entregar(servicio)).isEqualTo(Resultado.ENTREGADO);
        assertThat(llamadas).containsExactly(DESTINO);
    }

    @Test
    void elModoDirectoCaeAlReleSiFalla() throws Exception {
        CorreoService servicio = servicio(config("starttls", 587, "directo", true), null, null, null, (m, d) -> false, false);
        assertThat(entregar(servicio)).isEqualTo(Resultado.ENTREGADO);
        assertThat(registro.ultima().mensajes).hasSize(1);
    }

    @Test
    void elModoDirectoSinRespaldoEsPasajero() throws Exception {
        CorreoService servicio = servicio(config("starttls", 587, "directo", false), null, null, null, (m, d) -> false, false);
        assertThat(entregar(servicio)).isEqualTo(Resultado.FALLO_PASAJERO);
    }
}
