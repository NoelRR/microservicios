package com.toby.order.model;

// Ciclo de vida del pedido. Kitchen-service lo avanza al preparar/terminar.
public enum EstadoPedido {
    CREADO,          // recien registrado, espera cocina
    EN_PREPARACION,  // cocina lo tomo
    LISTO,           // listo para entregar
    ENTREGADO,       // entregado al cliente
    CANCELADO        // anulado
}
