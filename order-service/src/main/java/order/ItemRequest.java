package order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
        @NotNull(message = "El platoId es obligatorio")
        Long platoId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int cantidad
) {
}
