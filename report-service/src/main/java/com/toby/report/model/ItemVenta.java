package com.toby.report.model;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;

// Linea de una venta (UDT de Cassandra, embebida en la lista items de VentaRegistro).
// Se mapea al tipo CQL "item_venta" definido en infra/init-cassandra.cql.
@UserDefinedType("item_venta")
public class ItemVenta {

    private Long platoId;
    private int cantidad;

    public ItemVenta() {
    }

    public ItemVenta(Long platoId, int cantidad) {
        this.platoId = platoId;
        this.cantidad = cantidad;
    }

    public Long getPlatoId() { return platoId; }
    public void setPlatoId(Long platoId) { this.platoId = platoId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
