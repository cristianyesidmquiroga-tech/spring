package co.sena.adso.porteria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.sena.adso.porteria.dto.MovimientoRequestDTO;
import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.Equipo;
import co.sena.adso.porteria.entity.PuntoAcceso;
import co.sena.adso.porteria.entity.Rol;
import co.sena.adso.porteria.entity.Usuario;
import co.sena.adso.porteria.exception.ConflictoException;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.ObjetoExternoRepository;
import co.sena.adso.porteria.repository.PuntoAccesoRepository;
import co.sena.adso.porteria.repository.UsuarioRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PorteriaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VisitanteRepository visitanteRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ObjetoExternoRepository objetoRepository;
    @Mock private EquipoRepository equipoRepository;
    @Mock private AccesoRepository accesoRepository;
    @Mock private PuntoAccesoRepository puntoRepository;
    @Mock private AuthService authService;
    @Mock private AuditoriaService auditoriaService;

    private PorteriaService servicio;
    private Usuario celador;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-09-24T13:00:00Z"), ZoneId.of("America/Bogota"));
        servicio = new PorteriaService(usuarioRepository, visitanteRepository, vehiculoRepository, objetoRepository,
                equipoRepository, accesoRepository, puntoRepository, authService, auditoriaService, reloj);
        Rol rol = BeanUtils.instantiateClass(Rol.class);
        ReflectionTestUtils.setField(rol, "nombre", Rol.USUARIO);
        celador = new Usuario("Laura", "celador@x.co", "hash", rol, "Celador");
        ReflectionTestUtils.setField(celador, "id", 2L);
        lenient().when(authService.usuarioActual()).thenReturn(celador);
        lenient().when(usuarioRepository.existsById(4L)).thenReturn(true);
    }

    private void ultimoMovimiento(String tipo) {
        Acceso ultimo = tipo == null ? null
                : new Acceso(null, 4L, Acceso.USUARIO, tipo, LocalDateTime.of(2026, 9, 24, 7, 0), null, 2L);
        when(accesoRepository.findFirstByReferenciaIdAndTipoReferenciaOrderByFechaDescIdDesc(4L, Acceso.USUARIO))
                .thenReturn(Optional.ofNullable(ultimo));
    }

    @Test
    void salidaSinEntradaSeRechazaYQuedaEnAuditoria() {
        ultimoMovimiento(null);

        assertThatThrownBy(() -> servicio.registrarMovimiento(
                new MovimientoRequestDTO(Acceso.USUARIO, 4L, Acceso.SALIDA, List.of())))
                .isInstanceOf(ConflictoException.class);
        verify(auditoriaService).registrar(eq(celador), eq("accesos"), eq(4L), anyString(), any(), any(), anyString());
        verify(accesoRepository, never()).save(any());
    }

    @Test
    void dobleEntradaSeRechaza() {
        ultimoMovimiento(Acceso.ENTRADA);

        assertThatThrownBy(() -> servicio.registrarMovimiento(
                new MovimientoRequestDTO(Acceso.USUARIO, 4L, Acceso.ENTRADA, List.of())))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void entradaMarcaAdentroSoloLosEquiposDeEsaPersona() {
        ultimoMovimiento(Acceso.SALIDA);
        Equipo propio = new Equipo("Portátil", "S1", "Portátil", 4L);
        ReflectionTestUtils.setField(propio, "id", 10L);
        // El repositorio solo devuelve equipos de la persona; el 99 (ajeno) no viene
        when(equipoRepository.findByUsuarioIdAndIdIn(4L, List.of(10L, 99L))).thenReturn(List.of(propio));
        when(puntoRepository.findAll()).thenReturn(List.of(BeanUtils.instantiateClass(PuntoAcceso.class)));

        servicio.registrarMovimiento(new MovimientoRequestDTO(Acceso.USUARIO, 4L, Acceso.ENTRADA, List.of(10L, 99L)));

        assertThat(propio.getEstado()).isEqualTo(Equipo.ADENTRO);
        verify(accesoRepository).save(any(Acceso.class));
    }

    @Test
    void codigoDeVisitanteSeBuscaSinElPrefijo() {
        when(visitanteRepository.findByDocumento("52123456")).thenReturn(Optional.empty());

        assertThat(servicio.verificar(" SENA-VISIT:52123456 ").encontrado()).isFalse();
        verify(visitanteRepository).findByDocumento("52123456");
    }
}
