package menu;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "inventory.exchange";
    public static final String QUEUE_STOCK = "menu.stock-low";
    public static final String ROUTING_STOCK = "stock.low";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue stockQueue() {
        // durable: sobrevive reinicio del broker
        return new Queue(QUEUE_STOCK, true);
    }

    @Bean
    public Binding stockBinding(Queue stockQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(stockQueue).to(inventoryExchange).with(ROUTING_STOCK);
    }

    // Serializa/deserializa mensajes como JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
