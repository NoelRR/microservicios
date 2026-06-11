package com.toby.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Linea de un pedido. Guarda nombre y precio "congelados" al momento de la compra
// (snapshot): si menu sube el precio despues, el pedido historico no cambia.
@Entity
@Table(name = "items_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long platoId;

    @Column(nullable = false)
    private String nombrePlato;

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private BigDecimal subtotal;

    public ItemPedido() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlatoId() { return platoId; }
    public void setPlatoId(Long platoId) { this.platoId = platoId; }

    public String getNombrePlato() { return nombrePlato; }
    public void setNombrePlato(String nombrePlato) { this.nombrePlato = nombrePlato; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
