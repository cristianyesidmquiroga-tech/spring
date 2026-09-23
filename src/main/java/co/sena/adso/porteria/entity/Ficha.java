package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * El programa y la fecha de finalización se guardan una sola vez por ficha,
 * así todos los aprendices de la misma ficha salen igual en el carnet.
 */
@Entity
@Table(name = "fichas")
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(nullable = false, length = 150)
    private String programa;

    @Column(name = "fecha_finalizacion")
    private LocalDate fechaFinalizacion;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    protected Ficha() {
    }

    public String fechaFinalizacionTexto() {
        return fechaFinalizacion == null ? "" : fechaFinalizacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getPrograma() { return programa; }
    public LocalDate getFechaFinalizacion() { return fechaFinalizacion; }
    public boolean isActiva() { return activa; }
}
