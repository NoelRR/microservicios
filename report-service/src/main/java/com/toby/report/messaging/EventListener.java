package com.toby.report.messaging;

import com.toby.report.service.ReporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Consume order.created y lo agrega para reportes de ventas.
@Component
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final ReporteService reporteService;

    public EventListener(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ORDERS)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Venta registrada para reportes: pedido {}", event.pedidoId());
        reporteService.registrarVenta(event);
    }
}
