package co.sena.adso.porteria.service;

import co.sena.adso.porteria.exception.DatoInvalidoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Prueba de trabajo compatible con ALTCHA v1: no sale ningún dato hacia un tercero (Ley 1581)
@Service
public class CaptchaService {

    public static final String MENSAJE_ERROR = "No pudimos verificar que eres una persona. Recarga la página y vuelve a intentarlo.";
    private static final long VIGENCIA_SEGUNDOS = 15 * 60;

    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Long> usados = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final Clock reloj;
    private final byte[] clave;
    private boolean activo;
    private int dificultad;

    public CaptchaService(@Value("${app.captcha.activo:false}") boolean activo,
                          @Value("${app.captcha.dificultad:60000}") int dificultad,
                          @Value("${jwt.secret}") String secreto, ObjectMapper json, Clock reloj) {
        this.activo = activo;
        this.dificultad = dificultad;
        this.json = json;
        this.reloj = reloj;
        // Derivada del secreto de los tokens: no hay otra clave que administrar
        this.clave = hmac(secreto.getBytes(StandardCharsets.UTF_8), "captcha-desafio");
    }

    public boolean activo() {
        return activo;
    }

    // Apagado responde igual y no 404: así el formulario sabe que no debe esperar el widget
    public Map<String, Object> crearDesafio() {
        Map<String, Object> desafio = new LinkedHashMap<>();
        desafio.put("activo", activo);
        if (!activo) {
            return desafio;
        }
        int numero = azar.nextInt(dificultad + 1);
        byte[] sal = new byte[12];
        azar.nextBytes(sal);
        // expires va dentro del salt y lo cubre la firma: nadie alarga la vigencia sin invalidarlo
        String salt = HexFormat.of().formatHex(sal) + "?expires=" + (ahora() + VIGENCIA_SEGUNDOS) + "&";
        String reto = sha256(salt + numero);
        desafio.put("algorithm", "SHA-256");
        desafio.put("challenge", reto);
        desafio.put("maxNumber", dificultad);
        desafio.put("salt", salt);
        desafio.put("signature", firma(reto));
        return desafio;
    }

    public void validar(String solucion) {
        if (activo && !verificar(solucion)) {
            throw new DatoInvalidoException(MENSAJE_ERROR);
        }
    }

    private boolean verificar(String solucion) {
        if (solucion == null || solucion.isBlank()) {
            return false;
        }
        try {
            JsonNode datos = json.readTree(Base64.getDecoder().decode(solucion));
            String salt = datos.get("salt").asText();
            String reto = datos.get("challenge").asText();
            if (!iguales(datos.get("signature").asText(), firma(reto))
                    || !iguales(sha256(salt + datos.get("number").asLong()), reto)) {
                return false;
            }
            long expira = Long.parseLong(salt.substring(salt.indexOf("expires=") + 8, salt.lastIndexOf('&')));
            long ahora = ahora();
            if (expira < ahora) {
                return false;
            }
            // Una solución vale una sola vez: si no, un bot resuelve una y reenvía mil formularios
            usados.values().removeIf(vence -> vence < ahora);
            return usados.putIfAbsent(reto, ahora + VIGENCIA_SEGUNDOS) == null;
        } catch (Exception e) {
            return false;
        }
    }

    private long ahora() {
        return reloj.instant().getEpochSecond();
    }

    private String firma(String reto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(reto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] clave, String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean iguales(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
