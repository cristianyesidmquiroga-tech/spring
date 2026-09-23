package co.sena.adso.porteria.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarnetServiceTest {

    private final CarnetService carnet = new CarnetService();

    @Test
    void celadorSaleComoContratista() {
        assertThat(carnet.perfilDeCargo("Celador")).isEqualTo(CarnetService.CONTRATISTA);
        assertThat(carnet.perfilDeCargo("Portería")).isEqualTo(CarnetService.CONTRATISTA);
    }

    @Test
    void cargoDesconocidoSaleComoFuncionario() {
        assertThat(carnet.perfilDeCargo("Cualquiera")).isEqualTo(CarnetService.FUNCIONARIO);
        assertThat(carnet.perfilDeCargo(null)).isEqualTo(CarnetService.FUNCIONARIO);
    }

    @Test
    void conTresPalabrasUnaEsNombreYDosApellidos() {
        assertThat(carnet.partirNombre("Pedro Pérez Gómez", null, null))
                .containsExactly("Pedro", "Pérez Gómez");
    }

    @Test
    void conCuatroPalabrasSeRepartenMitadYMitad() {
        assertThat(carnet.partirNombre("Ana María Rueda Díaz", null, null))
                .containsExactly("Ana María", "Rueda Díaz");
    }

    @Test
    void siDeclaroNombresYApellidosSeUsanTalCual() {
        assertThat(carnet.partirNombre("Juan de la Cruz", "Juan", "de la Cruz"))
                .containsExactly("Juan", "de la Cruz");
    }
}
