package co.sena.adso.porteria.service;

import co.sena.adso.porteria.dto.ObjetoRequestDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.ObjetoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VehiculoDTO;
import co.sena.adso.porteria.dto.PasesResponseDTO.VisitanteDTO;
import co.sena.adso.porteria.dto.VehiculoRequestDTO;
import co.sena.adso.porteria.dto.VisitanteRequestDTO;
import co.sena.adso.porteria.entity.ObjetoExterno;
import co.sena.adso.porteria.entity.Vehiculo;
import co.sena.adso.porteria.entity.Visitante;
import co.sena.adso.porteria.exception.BusinessException;
import co.sena.adso.porteria.exception.ResourceNotFoundException;
import co.sena.adso.porteria.repository.ObjetoExternoRepository;
import co.sena.adso.porteria.repository.VehiculoRepository;
import co.sena.adso.porteria.repository.VisitanteRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pases manuales: si el documento, la placa o el serial ya existen, se renueva el pase en vez de duplicarlo. */
@Service
public class PaseService {

    private final VisitanteRepository visitanteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ObjetoExternoRepository objetoRepository;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public PaseService(VisitanteRepository visitanteRepository, VehiculoRepository vehiculoRepository,
                       ObjetoExternoRepository objetoRepository, Clock reloj) {
        this.visitanteRepository = visitanteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.objetoRepository = objetoRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PasesResponseDTO listar() {
        return new PasesResponseDTO(
                visitanteRepository.findTop200ByOrderByFechaCreacionDesc().stream().map(VisitanteDTO::fromEntity).toList(),
                vehiculoRepository.findTop200ByOrderByFechaCreacionDesc().stream().map(VehiculoDTO::fromEntity).toList(),
                objetoRepository.findTop200ByOrderByFechaCreacionDesc().stream().map(ObjetoDTO::fromEntity).toList());
    }

    @Transactional
    public VisitanteDTO registrarVisitante(VisitanteRequestDTO datos) {
        String documento = datos.documento().replaceAll("[\\s.\\-]", "").toUpperCase();
        String nombre = datos.nombre().trim();
        String motivo = opcional(datos.motivo());
        Visitante visitante = visitanteRepository.findByDocumento(documento)
                .map(v -> {
                    v.renovar(nombre, motivo);
                    return v;
                })
                .orElseGet(() -> visitanteRepository.save(new Visitante(nombre, documento, motivo, ahora())));
        return VisitanteDTO.fromEntity(visitante);
    }

    @Transactional
    public VehiculoDTO registrarVehiculo(VehiculoRequestDTO datos) {
        // Sin espacios ni guiones: "ABC-123" y "ABC 123" son la misma placa
        String placa = datos.placa().replaceAll("[\\s\\-]", "").toUpperCase();
        String propietario = opcional(datos.propietario());
        String motivo = opcional(datos.motivo());
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placa)
                .map(v -> {
                    v.renovar(datos.tipo(), propietario, motivo);
                    return v;
                })
                .orElseGet(() -> vehiculoRepository.save(new Vehiculo(placa, datos.tipo(), propietario, motivo, ahora())));
        return VehiculoDTO.fromEntity(vehiculo);
    }

    @Transactional
    public ObjetoDTO registrarObjeto(ObjetoRequestDTO datos) {
        String serial = opcional(datos.serial());
        String descripcion = datos.descripcion().trim();
        String propietario = opcional(datos.propietario());
        String motivo = opcional(datos.motivo());
        if (serial != null) {
            var existente = objetoRepository.findBySerial(serial);
            if (existente.isPresent()) {
                existente.get().actualizar(descripcion, propietario, motivo);
                return ObjetoDTO.fromEntity(existente.get());
            }
        } else {
            serial = serialGenerado();
        }
        return ObjetoDTO.fromEntity(objetoRepository.save(new ObjetoExterno(descripcion, serial, propietario, motivo, ahora())));
    }

    @Transactional
    public ObjetoDTO actualizarObjeto(Long id, ObjetoRequestDTO datos) {
        ObjetoExterno objeto = buscarObjeto(id);
        String serial = opcional(datos.serial());
        if (serial != null && !serial.equals(objeto.getSerial())) {
            throw new BusinessException("El serial no se puede cambiar porque va impreso en el código del pase");
        }
        objeto.actualizar(datos.descripcion().trim(), opcional(datos.propietario()), opcional(datos.motivo()));
        return ObjetoDTO.fromEntity(objeto);
    }

    @Transactional
    public ObjetoDTO desactivarObjeto(Long id) {
        ObjetoExterno objeto = buscarObjeto(id);
        objeto.desactivar();
        return ObjetoDTO.fromEntity(objeto);
    }

    private ObjetoExterno buscarObjeto(Long id) {
        return objetoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("un objeto externo", id));
    }

    // Fecha y un sufijo aleatorio: dos objetos registrados en el mismo segundo no chocan
    private String serialGenerado() {
        byte[] bytes = new byte[3];
        aleatorio.nextBytes(bytes);
        return "SN-" + ahora().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
