package order;

import java.math.BigDecimal;

// Vista parcial del Plato que devuelve menu-service. Solo lo que order necesita
// para validar y congelar precio. Campos extra del JSON se ignoran.
public record PlatoDTO(
        Long id,
        String nombre,
        BigDecimal precio,
        boolean activo,
        boolean disponible
) {
}
