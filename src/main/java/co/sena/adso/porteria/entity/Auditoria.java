package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Queda en NULL si se borra la cuenta; el nombre sigue guardado aparte
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre_usuario", nullable = false, length = 100)
    private String nombreUsuario;

    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Column(nullable = false)
    private String accion;

    @Column(name = "autorizado_por", length = 100)
    private String autorizadoPor;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Auditoria() {
    }

    public Auditoria(Usuario autor, String tablaAfectada, Long registroId, String accion,
                     String autorizadoPor, String motivo, String detalles, LocalDateTime fecha) {
        this.usuarioId = autor.getId();
        this.nombreUsuario = autor.getNombre();
        this.tablaAfectada = tablaAfectada;
        this.registroId = registroId;
        this.accion = accion;
        this.autorizadoPor = autorizadoPor;
        this.motivo = motivo;
        this.detalles = detalles;
        this.fecha = fecha;
    }

    public Auditoria(Long usuarioId, String nombreUsuario, String tablaAfectada, Long registroId, String accion,
                     String autorizadoPor, String motivo, String detalles, LocalDateTime fecha) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.tablaAfectada = tablaAfectada;
        this.registroId = registroId;
        this.accion = accion;
        this.autorizadoPor = autorizadoPor;
        this.motivo = motivo;
        this.detalles = detalles;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getTablaAfectada() { return tablaAfectada; }
    public Long getRegistroId() { return registroId; }
    public String getAccion() { return accion; }
    public String getAutorizadoPor() { return autorizadoPor; }
    public String getMotivo() { return motivo; }
    public String getDetalles() { return detalles; }
    public LocalDateTime getFecha() { return fecha; }
}
