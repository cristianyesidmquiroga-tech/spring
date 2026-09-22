package co.sena.adso.fincasapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fincas")
public class Finca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String propietario;

    @Column(nullable = false, length = 100)
    private String vereda;

    @Column(nullable = false, length = 100)
    private String municipio;

    @Column(nullable = false)
    private Double hectareas;

    protected Finca() {
    }

    public Finca(String nombre, String propietario, String vereda, String municipio, Double hectareas) {
        this.nombre = nombre;
        this.propietario = propietario;
        this.vereda = vereda;
        this.municipio = municipio;
        this.hectareas = hectareas;
    }

    public void actualizar(String nombre, String propietario, String vereda, String municipio, Double hectareas) {
        this.nombre = nombre;
        this.propietario = propietario;
        this.vereda = vereda;
        this.municipio = municipio;
        this.hectareas = hectareas;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getPropietario() { return propietario; }
    public String getVereda() { return vereda; }
    public String getMunicipio() { return municipio; }
    public Double getHectareas() { return hectareas; }
}
