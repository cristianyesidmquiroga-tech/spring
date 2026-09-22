package co.sena.adso.fincasapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.sena.adso.fincasapi.dto.FincaCultivoRequestDTO;
import co.sena.adso.fincasapi.dto.FincaCultivoResponseDTO;
import co.sena.adso.fincasapi.entity.Cultivo;
import co.sena.adso.fincasapi.entity.Finca;
import co.sena.adso.fincasapi.entity.FincaCultivo;
import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.enums.Temporada;
import co.sena.adso.fincasapi.exception.BusinessException;
import co.sena.adso.fincasapi.repository.FincaCultivoRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FincaCultivoServiceTest {

    @Mock
    private FincaCultivoRepository siembraRepository;

    @Mock
    private FincaService fincaService;

    @Mock
    private CultivoService cultivoService;

    @InjectMocks
    private FincaCultivoService siembraService;

    private Finca finca;
    private Cultivo maiz;

    @BeforeEach
    void prepararDatos() {
        finca = new Finca("El Recreo", "Luis", "Alto Jordán", "Vélez", 10.0);
        ReflectionTestUtils.setField(finca, "id", 1L);
        maiz = new Cultivo("Maíz", "transitorio", 120);
        ReflectionTestUtils.setField(maiz, "id", 3L);
        when(fincaService.buscarFinca(1L)).thenReturn(finca);
        when(cultivoService.buscarCultivo(3L)).thenReturn(maiz);
    }

    private FincaCultivoRequestDTO siembra(double area, EstadoSiembra estado) {
        return new FincaCultivoRequestDTO(1L, 3L, area, LocalDate.of(2026, 3, 15), Temporada.PRIMAVERA, estado);
    }

    @Test
    void rechazaUnaSiembraMasGrandeQueLaFinca() {
        assertThatThrownBy(() -> siembraService.crear(siembra(999.0, EstadoSiembra.ACTIVO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor que la finca");
        verify(siembraRepository, never()).save(any());
    }

    @Test
    void sumaLoQueYaEstaSembradoAntesDeAceptar() {
        when(siembraRepository.sumarArea(1L, EstadoSiembra.ACTIVO, -1L)).thenReturn(7.0);

        assertThatThrownBy(() -> siembraService.crear(siembra(4.0, EstadoSiembra.ACTIVO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quedan 3.0 ha libres");
    }

    @Test
    void guardaCuandoElAreaCabe() {
        when(siembraRepository.sumarArea(1L, EstadoSiembra.ACTIVO, -1L)).thenReturn(5.0);
        when(siembraRepository.save(any(FincaCultivo.class))).thenAnswer(inv -> inv.getArgument(0));

        FincaCultivoResponseDTO creada = siembraService.crear(siembra(5.0, EstadoSiembra.ACTIVO));

        assertThat(creada.finca()).isEqualTo("El Recreo");
        assertThat(creada.cultivo()).isEqualTo("Maíz");
        assertThat(creada.areaSembradaHa()).isEqualTo(5.0);
    }

    @Test
    void unaSiembraCosechadaNoOcupaTerreno() {
        when(siembraRepository.save(any(FincaCultivo.class))).thenAnswer(inv -> inv.getArgument(0));

        siembraService.crear(siembra(8.0, EstadoSiembra.COSECHADO));

        verify(siembraRepository, never()).sumarArea(any(), any(), any());
    }
}
