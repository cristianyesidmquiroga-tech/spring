package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Un hilo por persona: todos los asesores ven y responden el mismo
@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Queda en NULL si se borra la cuenta; el nombre sigue guardado aparte
    @Column(name = "autor_id")
    private Long autorId;

    @Column(name = "autor_nombre", nullable = false, length = 100)
    private String autorNombre;

    @Column(name = "autor_es_admin", nullable = false)
    private boolean autorEsAdmin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leido;

    @Column(nullable = false)
    private boolean automatico;

    protected Mensaje() {
    }

    public Mensaje(Long usuarioId, Usuario autor, String texto, boolean automatico, LocalDateTime fecha) {
        this.usuarioId = usuarioId;
        this.autorId = autor.getId();
        this.autorNombre = autor.getNombre();
        this.autorEsAdmin = autor.puedeAsesorar();
        this.texto = texto;
        this.automatico = automatico;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getAutorId() { return autorId; }
    public String getAutorNombre() { return autorNombre; }
    public boolean isAutorEsAdmin() { return autorEsAdmin; }
    public String getTexto() { return texto; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isLeido() { return leido; }
    public boolean isAutomatico() { return automatico; }
}
