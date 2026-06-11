package com.toby.order.messaging;

import java.math.BigDecimal;
import java.util.List;

// Evento publicado al crear un pedido. Lo consumen:
//  - kitchen-service: para encolar la preparacion.
//  - inventory-service: para descontar ingredientes.
public record OrderEvent(
        Long pedidoId,
        String clienteEmail,
        BigDecimal total,
        List<Linea> items
) {
    // Linea minima para los consumidores: que plato y cuanto.
    public record Linea(Long platoId, int cantidad) {
    }
}
