package co.sena.adso.porteria.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Rol {

    public static final String ADMIN = "Admin";
    public static final String USUARIO = "Usuario";
    public static final String TRABAJADOR = "Trabajador";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    protected Rol() {
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
