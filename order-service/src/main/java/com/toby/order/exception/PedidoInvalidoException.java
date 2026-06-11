package com.toby.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Plato inexistente, no disponible, o no se pudo validar contra menu-service.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PedidoInvalidoException extends RuntimeException {
    public PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
