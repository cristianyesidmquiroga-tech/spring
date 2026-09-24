package co.sena.adso.porteria.service;

import java.util.regex.Pattern;

// Limpieza de entrada, no defensa contra XSS: el escape real lo hace React al pintar
public final class Texto {

    private static final Pattern ETIQUETA = Pattern.compile("<[^>]*?>");

    private Texto() {
    }

    public static String limpiar(String valor) {
        return valor == null ? null : ETIQUETA.matcher(valor).replaceAll("").trim();
    }

    public static String opcional(String valor) {
        String limpio = limpiar(valor);
        return limpio == null || limpio.isBlank() ? null : limpio;
    }
}
