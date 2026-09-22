package co.sena.adso.fincasapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.sena.adso.fincasapi.dto.FincaRequestDTO;
import co.sena.adso.fincasapi.dto.FincaResponseDTO;
import co.sena.adso.fincasapi.entity.Finca;
import co.sena.adso.fincasapi.exception.ResourceNotFoundException;
import co.sena.adso.fincasapi.repository.FincaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FincaServiceTest {

    @Mock
    private FincaRepository fincaRepository;

    @InjectMocks
    private FincaService fincaService;

    @Test
    void obtenerLanzaNotFoundSiLaFincaNoExiste() {
        when(fincaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fincaService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearLimpiaEspaciosYGuarda() {
        when(fincaRepository.save(any(Finca.class))).thenAnswer(inv -> inv.getArgument(0));

        FincaResponseDTO creada = fincaService.crear(
                new FincaRequestDTO("  La Aurora ", "Ana", "El Centro", "Vélez", 6.5));

        assertThat(creada.nombre()).isEqualTo("La Aurora");
        assertThat(creada.hectareas()).isEqualTo(6.5);
    }

    @Test
    void actualizarCambiaLosDatosDeLaFinca() {
        Finca finca = new Finca("La Aurora", "Ana", "El Centro", "Vélez", 6.5);
        when(fincaRepository.findById(1L)).thenReturn(Optional.of(finca));

        FincaResponseDTO actualizada = fincaService.actualizar(1L,
                new FincaRequestDTO("La Aurora renovada", "Ana", "El Centro", "Vélez", 7.0));

        assertThat(actualizada.nombre()).isEqualTo("La Aurora renovada");
        assertThat(finca.getHectareas()).isEqualTo(7.0);
    }

    @Test
    void eliminarUnaFincaInexistenteNoBorraNada() {
        when(fincaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fincaService.eliminar(7L)).isInstanceOf(ResourceNotFoundException.class);
        verify(fincaRepository, never()).delete(any(Finca.class));
    }
}
