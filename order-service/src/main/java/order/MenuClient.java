package order;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Consulta menu-service para validar platos y obtener su precio actual.
// Resuelve la instancia via Eureka (lb) usando el nombre logico "menu-service".
@Component
public class MenuClient {

    private final RestClient restClient;

    public MenuClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder
                .baseUrl("http://menu-service")
                .build();
    }

    // Trae el plato por id. Lanza PedidoInvalidoException si no existe o menu no responde.
    public PlatoDTO obtenerPlato(Long platoId) {
        try {
            PlatoDTO plato = restClient.get()
                    .uri("/menu/items/{id}", platoId)
                    .retrieve()
                    .body(PlatoDTO.class);
            if (plato == null) {
                throw new PedidoInvalidoException("Plato no encontrado: " + platoId);
            }
            return plato;
        } catch (RestClientException e) {
            // 404 de menu o menu caido -> el pedido no se puede validar.
            throw new PedidoInvalidoException(
                    "No se pudo validar el plato " + platoId + ": " + e.getMessage());
        }
    }
}
