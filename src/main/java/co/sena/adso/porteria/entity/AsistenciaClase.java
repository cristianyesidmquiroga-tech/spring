package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencia_clases")
public class AsistenciaClase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Queda en NULL si se borra la cuenta del instructor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Usuario instructor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aprendiz_id")
    private Usuario aprendiz;

    @Column(nullable = false, length = 20)
    private String ficha;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean presente;

    private String evaluacion;

    protected AsistenciaClase() {
    }

    public AsistenciaClase(Usuario instructor, Usuario aprendiz, String ficha, LocalDateTime fecha, boolean presente) {
        this.instructor = instructor;
        this.aprendiz = aprendiz;
        this.ficha = ficha;
        this.fecha = fecha;
        this.presente = presente;
    }

    public Long getId() { return id; }
    public Usuario getInstructor() { return instructor; }
    public Usuario getAprendiz() { return aprendiz; }
    public String getFicha() { return ficha; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isPresente() { return presente; }
    public String getEvaluacion() { return evaluacion; }
}
