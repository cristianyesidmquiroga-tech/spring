package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "equipos")
public class Equipo {

    public static final List<String> TIPOS = List.of("Portátil", "Computador", "Tablet", "Celular", "Otro");
    public static final int MAXIMO_POR_USUARIO = 5;
    public static final String ADENTRO = "Adentro";
    public static final String AFUERA = "Afuera";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, length = 60)
    private String serial;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 10)
    private String estado = AFUERA;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    protected Equipo() {
    }

    public Equipo(String nombre, String serial, String tipo, Long usuarioId) {
        this.nombre = nombre;
        this.serial = serial;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSerial() { return serial; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public Long getUsuarioId() { return usuarioId; }
}
