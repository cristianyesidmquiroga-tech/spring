package co.sena.adso.porteria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Una entrada o salida. referencia_id apunta a la tabla que indique tipo_referencia,
 * por eso toda consulta debe filtrar por los dos campos juntos.
 */
@Entity
@Table(name = "accesos")
public class Acceso {

    public static final String USUARIO = "Usuario";
    public static final String VISITANTE = "Visitante";
    public static final String VEHICULO = "Vehiculo";
    public static final String OBJETO = "ObjetoExterno";
    public static final List<String> TIPOS_REFERENCIA = List.of(USUARIO, VISITANTE, VEHICULO, OBJETO);

    public static final String ENTRADA = "Entrada";
    public static final String SALIDA = "Salida";
    public static final String AFUERA = "Afuera";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_id")
    private PuntoAcceso punto;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(name = "tipo_referencia", nullable = false, length = 20)
    private String tipoReferencia;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Ids de los equipos que pasaron con la persona, separados por coma
    @Column(name = "equipos_ids", length = 100)
    private String equiposIds;

    // Quién lo registró; es null en las salidas del cierre automático de medianoche
    @Column(name = "operador_id")
    private Long operadorId;

    protected Acceso() {
    }

    public Acceso(PuntoAcceso punto, Long referenciaId, String tipoReferencia, String tipo,
                  LocalDateTime fecha, String equiposIds, Long operadorId) {
        this.punto = punto;
        this.referenciaId = referenciaId;
        this.tipoReferencia = tipoReferencia;
        this.tipo = tipo;
        this.fecha = fecha;
        this.equiposIds = equiposIds;
        this.operadorId = operadorId;
    }

    public List<Long> listaEquipos() {
        if (equiposIds == null || equiposIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(equiposIds.split(","))
                .map(String::trim)
                .filter(s -> s.chars().allMatch(Character::isDigit) && !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    public Long getId() { return id; }
    public PuntoAcceso getPunto() { return punto; }
    public Long getReferenciaId() { return referenciaId; }
    public String getTipoReferencia() { return tipoReferencia; }
    public String getTipo() { return tipo; }
    public LocalDateTime getFecha() { return fecha; }
    public String getEquiposIds() { return equiposIds; }
    public Long getOperadorId() { return operadorId; }
}
