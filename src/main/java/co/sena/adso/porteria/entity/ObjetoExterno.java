package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "objetos_externos")
public class ObjetoExterno {

    public static final String PREFIJO = "SENA-OBJ:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, unique = true, length = 60)
    private String serial;

    @Column(length = 100)
    private String propietario;

    private String motivo;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected ObjetoExterno() {
    }

    public ObjetoExterno(String descripcion, String serial, String propietario, String motivo, LocalDateTime fecha) {
        this.serial = serial;
        this.codigo = PREFIJO + serial;
        this.fechaCreacion = fecha;
        actualizar(descripcion, propietario, motivo);
    }

    public void actualizar(String descripcion, String propietario, String motivo) {
        this.descripcion = descripcion;
        this.propietario = propietario;
        this.motivo = motivo;
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public String getSerial() { return serial; }
    public String getPropietario() { return propietario; }
    public String getMotivo() { return motivo; }
    public String getCodigo() { return codigo; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
