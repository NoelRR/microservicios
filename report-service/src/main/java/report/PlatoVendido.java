package report;

// Resultado de la agregacion "platos mas vendidos". platoId = _id del group.
public class PlatoVendido {

    private Long platoId;
    private long unidades;

    public PlatoVendido() {
    }

    public Long getPlatoId() { return platoId; }
    public void setPlatoId(Long platoId) { this.platoId = platoId; }

    public long getUnidades() { return unidades; }
    public void setUnidades(long unidades) { this.unidades = unidades; }
}
