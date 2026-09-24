package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitantes")
public class Visitante {

    public static final String PREFIJO = "SENA-VISIT:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    private String motivo;

    // Lo que va en el código QR del pase
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Visitante() {
    }

    public Visitante(String nombre, String documento, String motivo, LocalDateTime fecha) {
        this.nombre = nombre;
        this.documento = documento;
        this.motivo = motivo;
        this.codigo = PREFIJO + documento;
        this.fechaCreacion = fecha;
    }

    public void renovar(String nombre, String motivo) {
        this.nombre = nombre;
        this.motivo = motivo;
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDocumento() { return documento; }
    public String getMotivo() { return motivo; }
    public String getCodigo() { return codigo; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
