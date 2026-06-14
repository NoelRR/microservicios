package menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockListener {

    private static final Logger log = LoggerFactory.getLogger(StockListener.class);

    private final MenuService menuService;

    public StockListener(MenuService menuService) {
        this.menuService = menuService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_STOCK)
    public void onStockEvent(StockEvent event) {
        log.info("Evento stock recibido: plato={} disponible={}", event.platoId(), event.disponible());
        menuService.marcarDisponibilidad(event.platoId(), event.disponible());
    }
}
