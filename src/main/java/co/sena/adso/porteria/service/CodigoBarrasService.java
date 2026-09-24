package co.sena.adso.porteria.service;

import co.sena.adso.porteria.exception.DatoInvalidoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Code128-B dibujado como SVG. Code128 porque los pases mezclan letras, dígitos y ':'
 * (SENA-VISIT:..., SENA-VEH-E:...) y es lo que lee el escáner de portería.
 */
@Service
public class CodigoBarrasService {

    public static final List<String> PATRONES = List.of(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213",
        "122312", "132212", "221213", "221312", "231212", "112232", "122132",
        "122231", "113222", "123122", "123221", "223211", "221132", "221231",
        "213212", "223112", "312131", "311222", "321122", "321221", "312212",
        "322112", "322211", "212123", "212321", "232121", "111323", "131123",
        "131321", "112313", "132113", "132311", "211313", "231113", "231311",
        "112133", "112331", "132131", "113123", "113321", "133121", "313121",
        "211331", "231131", "213113", "213311", "213131", "311123", "311321",
        "331121", "312113", "312311", "332111", "314111", "221411", "431111",
        "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114",
        "413111", "241112", "134111", "111242", "121142", "121241", "114212",
        "124112", "124211", "411212", "421112", "421211", "212141", "214121",
        "412121", "111143", "111341", "131141", "114113", "114311", "411113",
        "411311", "113141", "114131", "311141", "411131", "211412", "211214",
        "211232", "2331112");

    public static final int INICIO_B = 104;
    public static final int PARADA = 106;
    private static final int ALTO = 70;
    // La norma pide al menos 10 módulos de margen blanco para que el lector encuentre el inicio
    private static final int ZONA_MUDA = 12;

    /** Valores del símbolo con Start B, dígito de control y Stop. Solo ASCII imprimible (32..126). */
    public List<Integer> valores(String dato) {
        if (dato == null || dato.isEmpty()) {
            throw new DatoInvalidoException("No hay dato que codificar");
        }
        List<Integer> valores = new ArrayList<>();
        valores.add(INICIO_B);
        for (char c : dato.toCharArray()) {
            if (c < 32 || c > 126) {
                throw new DatoInvalidoException("El carácter \"" + c + "\" no se puede representar en Code128");
            }
            valores.add(c - 32);
        }
        int suma = INICIO_B;
        for (int i = 1; i < valores.size(); i++) {
            suma += valores.get(i) * i;
        }
        valores.add(suma % 103);
        valores.add(PARADA);
        return valores;
    }

    /** Anchos alternando barra y espacio, empezando por barra. */
    public List<Integer> modulos(String dato) {
        List<Integer> anchos = new ArrayList<>();
        for (int valor : valores(dato)) {
            for (char c : PATRONES.get(valor).toCharArray()) {
                anchos.add(c - '0');
            }
        }
        return anchos;
    }

    public String svg(String dato) {
        return svg(dato, true);
    }

    // Sin ancho fijo, solo viewBox: el CSS lo estira a todo el ancho, que es lo que necesita un lector desde un celular
    public String svg(String dato, boolean mostrarTexto) {
        List<Integer> anchos = modulos(dato);
        int anchoTotal = anchos.stream().mapToInt(Integer::intValue).sum() + ZONA_MUDA * 2;
        int altoSvg = ALTO + (mostrarTexto ? 14 : 0);

        StringBuilder barras = new StringBuilder();
        int posicion = ZONA_MUDA;
        boolean esBarra = true;
        for (int ancho : anchos) {
            if (esBarra) {
                barras.append("<rect x=\"").append(posicion).append("\" y=\"0\" width=\"").append(ancho)
                        .append("\" height=\"").append(ALTO).append("\" fill=\"#000\"/>");
            }
            posicion += ancho;
            esBarra = !esBarra;
        }
        String texto = !mostrarTexto ? "" : String.format(Locale.ROOT,
                "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"11\" "
                        + "fill=\"#000\" letter-spacing=\"1\">%s</text>", anchoTotal / 2.0, altoSvg - 2, escapar(dato));
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + anchoTotal + " " + altoSvg
                + "\" preserveAspectRatio=\"none\" role=\"img\" aria-label=\"Código de barras " + escapar(dato) + "\">"
                + "<rect x=\"0\" y=\"0\" width=\"" + anchoTotal + "\" height=\"" + altoSvg + "\" fill=\"#fff\"/>"
                + barras + texto + "</svg>";
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
