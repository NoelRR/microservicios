package report;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private final VentaRepository ventaRepository;

    public ReporteService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    // ---------- INGESTA (desde eventos) ----------

    // Idempotente: no duplica la venta si el evento se reentrega.
    public void registrarVenta(OrderCreatedEvent event) {
        if (ventaRepository.existsByPedidoId(event.pedidoId())) {
            return;
        }
        VentaRegistro venta = new VentaRegistro();
        venta.setPedidoId(event.pedidoId());
        venta.setClienteEmail(event.clienteEmail());
        venta.setTotal(event.total());
        venta.setItems(event.items().stream()
                .map(l -> new ItemVenta(l.platoId(), l.cantidad()))
                .toList());
        ventaRepository.save(venta);
    }

    // ---------- REPORTES ----------

    public ResumenVentas resumenVentas() {
        List<VentaRegistro> ventas = ventaRepository.findAll();
        long totalPedidos = ventas.size();
        BigDecimal ingreso = ventas.stream()
                .map(VentaRegistro::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = (totalPedidos == 0)
                ? BigDecimal.ZERO
                : ingreso.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);
        return new ResumenVentas(totalPedidos, ingreso, ticket);
    }

    // Top N platos por unidades vendidas. Cassandra no hace unwind/group, asi que
    // se agrega en memoria sobre el scan de ventas (volumen academico, OK).
    public List<PlatoVendido> platosMasVendidos(int limite) {
        Map<Long, Long> unidadesPorPlato = new HashMap<>();
        for (VentaRegistro venta : ventaRepository.findAll()) {
            if (venta.getItems() == null) continue;
            for (ItemVenta item : venta.getItems()) {
                unidadesPorPlato.merge(item.getPlatoId(), (long) item.getCantidad(), Long::sum);
            }
        }
        return unidadesPorPlato.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limite)
                .map(e -> {
                    PlatoVendido p = new PlatoVendido();
                    p.setPlatoId(e.getKey());
                    p.setUnidades(e.getValue());
                    return p;
                })
                .toList();
    }
}
