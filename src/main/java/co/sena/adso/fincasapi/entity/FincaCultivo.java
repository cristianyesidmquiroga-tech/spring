package co.sena.adso.fincasapi.entity;

import co.sena.adso.fincasapi.enums.EstadoSiembra;
import co.sena.adso.fincasapi.enums.Temporada;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Siembra de un cultivo en una finca. Es una entidad aparte (y no un simple
 * @ManyToMany) porque la relación guarda sus propios datos: área, fecha y estado.
 */
@Entity
@Table(name = "finca_cultivo")
public class FincaCultivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finca_id")
    private Finca finca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cultivo_id")
    private Cultivo cultivo;

    @Column(name = "area_sembrada_ha", nullable = false)
    private Double areaSembradaHa;

    @Column(name = "fecha_siembra", nullable = false)
    private LocalDate fechaSiembra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Temporada temporada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSiembra estado;

    protected FincaCultivo() {
    }

    public FincaCultivo(Finca finca, Cultivo cultivo, Double areaSembradaHa,
                        LocalDate fechaSiembra, Temporada temporada, EstadoSiembra estado) {
        this.finca = finca;
        this.cultivo = cultivo;
        this.areaSembradaHa = areaSembradaHa;
        this.fechaSiembra = fechaSiembra;
        this.temporada = temporada;
        this.estado = estado;
    }

    public void actualizar(Finca finca, Cultivo cultivo, Double areaSembradaHa,
                           LocalDate fechaSiembra, Temporada temporada, EstadoSiembra estado) {
        this.finca = finca;
        this.cultivo = cultivo;
        this.areaSembradaHa = areaSembradaHa;
        this.fechaSiembra = fechaSiembra;
        this.temporada = temporada;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public Finca getFinca() { return finca; }
    public Cultivo getCultivo() { return cultivo; }
    public Double getAreaSembradaHa() { return areaSembradaHa; }
    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public Temporada getTemporada() { return temporada; }
    public EstadoSiembra getEstado() { return estado; }
}
