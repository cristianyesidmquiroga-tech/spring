package co.sena.adso.porteria.service;

import co.sena.adso.porteria.entity.Acceso;
import co.sena.adso.porteria.entity.PuntoAcceso;
import co.sena.adso.porteria.repository.AccesoRepository;
import co.sena.adso.porteria.repository.EquipoRepository;
import co.sena.adso.porteria.repository.PuntoAccesoRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A medianoche se registra la salida de todo lo que quedó adentro y se cierran los pases del día.
 * Todo va en una transacción: o se cierra completo o no se cierra nada.
 */
@Service
public class CierreNocturnoService {

    private static final Logger log = LoggerFactory.getLogger(CierreNocturnoService.class);

    private final AccesoRepository accesoRepository;
    private final PuntoAccesoRepository puntoRepository;
    private final VisitanteRepository visitanteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final EquipoRepository equipoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock reloj;

    public CierreNocturnoService(AccesoRepository accesoRepository, PuntoAccesoRepository puntoRepository,
                                 VisitanteRepository visitanteRepository, VehiculoRepository vehiculoRepository,
                                 EquipoRepository equipoRepository, AuditoriaService auditoriaService, Clock reloj) {
        this.accesoRepository = accesoRepository;
        this.puntoRepository = puntoRepository;
        this.visitanteRepository = visitanteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.equipoRepository = equipoRepository;
        this.auditoriaService = auditoriaService;
        this.reloj = reloj;
    }

    @Scheduled(cron = "5 0 0 * * *", zone = "America/Bogota")
    @Transactional
    public void cerrarDia() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        // Corre pasada la medianoche: las salidas pendientes son del día anterior, a las 23:59:59
        LocalDateTime cierre = ahora.getHour() < 1
                ? LocalDate.from(ahora).minusDays(1).atTime(LocalTime.of(23, 59, 59))
                : ahora.withNano(0);

        int visitantes = visitanteRepository.desactivarTodos();
        int vehiculos = vehiculoRepository.desactivarTodos();
        int equipos = equipoRepository.sacarTodos();

        List<Object[]> pendientes = accesoRepository.pendientesDeSalida();
        for (Object[] fila : pendientes) {
            PuntoAcceso punto = puntoRepository.getReferenceById(((Number) fila[2]).longValue());
            // Sin operador: así el historial distingue el cierre automático de una salida real
            accesoRepository.save(new Acceso(punto, ((Number) fila[0]).longValue(), (String) fila[1],
                    Acceso.SALIDA, cierre, null, null));
        }

        auditoriaService.registrarSistema("varias (cierre nocturno)", "Cierre automático de ingresos a medianoche",
                "Visitantes: " + visitantes + ", vehículos: " + vehiculos + ", equipos: " + equipos
                        + ", salidas registradas: " + pendientes.size(), cierre);
        log.info("Cierre nocturno: {} salidas registradas", pendientes.size());
    }
}
