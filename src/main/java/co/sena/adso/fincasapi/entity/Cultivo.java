package co.sena.adso.fincasapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cultivos")
public class Cultivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "ciclo_dias", nullable = false)
    private Integer cicloDias;

    protected Cultivo() {
    }

    public Cultivo(String nombre, String tipo, Integer cicloDias) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.cicloDias = cicloDias;
    }

    public void actualizar(String nombre, String tipo, Integer cicloDias) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.cicloDias = cicloDias;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public Integer getCicloDias() { return cicloDias; }
}
