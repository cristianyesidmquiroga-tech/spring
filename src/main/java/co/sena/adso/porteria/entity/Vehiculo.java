package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    public static final String SENA = "SENA";
    public static final String EXTERNO = "Externo";
    public static final List<String> TIPOS = List.of(SENA, EXTERNO);
    public static final String PREFIJO_SENA = "SENA-VEH-S:";
    public static final String PREFIJO_EXTERNO = "SENA-VEH-E:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(length = 100)
    private String propietario;

    private String motivo;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Vehiculo() {
    }

    public Vehiculo(String placa, String tipo, String propietario, String motivo, LocalDateTime fecha) {
        this.placa = placa;
        this.fechaCreacion = fecha;
        renovar(tipo, propietario, motivo);
    }

    public void renovar(String tipo, String propietario, String motivo) {
        this.tipo = tipo;
        this.propietario = propietario;
        this.motivo = motivo;
        this.codigo = (SENA.equals(tipo) ? PREFIJO_SENA : PREFIJO_EXTERNO) + placa;
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getPlaca() { return placa; }
    public String getTipo() { return tipo; }
    public String getPropietario() { return propietario; }
    public String getMotivo() { return motivo; }
    public String getCodigo() { return codigo; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
