package co.sena.adso.porteria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.adso.porteria.exception.DatoInvalidoException;
import org.junit.jupiter.api.Test;

class DocumentoServiceTest {

    private final DocumentoService documentos = new DocumentoService();

    @Test
    void quitaPuntosYEspaciosAlValidar() {
        assertThat(documentos.validar("CC", "1.098 765-432")).isEqualTo("1098765432");
    }

    @Test
    void cedulaNoPuedeEmpezarPorCero() {
        assertThatThrownBy(() -> documentos.validar("CC", "0123456"))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("no empieza por cero");
    }

    @Test
    void cedulaDeExtranjeriaSiPuedeEmpezarPorCero() {
        assertThat(documentos.validar("CE", "012345")).isEqualTo("012345");
    }

    @Test
    void tarjetaDeIdentidadExigeDiezUOnceDigitos() {
        assertThatThrownBy(() -> documentos.validar("TI", "12345678"))
                .isInstanceOf(DatoInvalidoException.class)
                .hasMessageContaining("entre 10 y 11");
    }

    @Test
    void pasaporteAdmiteLetras() {
        assertThat(documentos.validar("PA", "AB12345")).isEqualTo("AB12345");
    }

    @Test
    void tipoDesconocidoSeRechaza() {
        assertThatThrownBy(() -> documentos.validar("XX", "123456")).isInstanceOf(DatoInvalidoException.class);
    }
}
