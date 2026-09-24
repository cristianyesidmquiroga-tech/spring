package co.sena.adso.porteria.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "puntos_acceso")
public class PuntoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    protected PuntoAcceso() {
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
}
