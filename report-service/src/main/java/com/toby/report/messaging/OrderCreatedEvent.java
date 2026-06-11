package com.toby.report.messaging;

import java.math.BigDecimal;
import java.util.List;

// Espejo del OrderEvent de order-service.
public record OrderCreatedEvent(
        Long pedidoId,
        String clienteEmail,
        BigDecimal total,
        List<Linea> items
) {
    public record Linea(Long platoId, int cantidad) {
    }
}
