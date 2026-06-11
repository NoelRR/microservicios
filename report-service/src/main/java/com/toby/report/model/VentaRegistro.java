package com.toby.report.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Registro de venta derivado de order.created. Base de los reportes.
// Tabla Cassandra: pedidoId es la clave de particion (un pedido = una fila).
// Como pedidoId es PK, save() es naturalmente idempotente (upsert).
@Table("ventas")
public class VentaRegistro {

    @PrimaryKey
    private Long pedidoId;

    private String clienteEmail;

    private BigDecimal total;

    // list<frozen<item_venta>> en CQL.
    private List<ItemVenta> items;

    private Instant fecha = Instant.now();

    public VentaRegistro() {
    }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<ItemVenta> getItems() { return items; }
    public void setItems(List<ItemVenta> items) { this.items = items; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
