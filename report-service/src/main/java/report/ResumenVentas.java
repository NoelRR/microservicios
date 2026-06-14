package report;

import java.math.BigDecimal;

// Resumen agregado de ventas.
public record ResumenVentas(
        long totalPedidos,
        BigDecimal ingresoTotal,
        BigDecimal ticketPromedio
) {
}
