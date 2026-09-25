package co.sena.adso.porteria.soporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.mock.web.MockMultipartFile;

// Como excel_de() de Portería 2: un .xlsx de verdad, para que el fallo se vea al leerlo por la ruta real
public final class Excel {

    private Excel() {
    }

    public static MockMultipartFile deFilas(List<Map<String, Object>> filas, String nombre) throws IOException {
        Set<String> columnas = new LinkedHashSet<>();
        filas.forEach(f -> columnas.addAll(f.keySet()));
        List<String> titulos = new ArrayList<>(columnas);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Workbook libro = new Workbook(salida, "pruebas", "1.0");
        Worksheet hoja = libro.newWorksheet("Hoja1");
        for (int c = 0; c < titulos.size(); c++) {
            hoja.value(0, c, titulos.get(c));
        }
        for (int r = 0; r < filas.size(); r++) {
            for (int c = 0; c < titulos.size(); c++) {
                Object valor = filas.get(r).get(titulos.get(c));
                if (valor instanceof Number n) {
                    hoja.value(r + 1, c, n);
                } else if (valor != null) {
                    hoja.value(r + 1, c, valor.toString());
                }
            }
        }
        libro.finish();
        return new MockMultipartFile("archivo", nombre,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", salida.toByteArray());
    }
}
