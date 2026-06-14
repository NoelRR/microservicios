package order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PedidoRequest(
        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "Email invalido")
        String clienteEmail,

        @NotEmpty(message = "El pedido debe tener al menos un item")
        @Valid
        List<ItemRequest> items
) {
}
