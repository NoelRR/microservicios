package order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MenuClient menuClient;
    private final OrderPublisher orderPublisher;

    public PedidoService(PedidoRepository pedidoRepository,
                         MenuClient menuClient,
                         OrderPublisher orderPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.menuClient = menuClient;
        this.orderPublisher = orderPublisher;
    }

    // Crea el pedido: valida cada plato contra menu-service, congela precios,
    // calcula total, persiste y publica order.created.
    @Transactional
    public Pedido crearPedido(PedidoRequest req) {
        Pedido pedido = new Pedido();
        pedido.setClienteEmail(req.clienteEmail());

        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : req.items()) {
            PlatoDTO plato = menuClient.obtenerPlato(item.platoId());

            // No se puede pedir algo oculto o sin stock.
            if (!plato.activo() || !plato.disponible()) {
                throw new PedidoInvalidoException(
                        "El plato '" + plato.nombre() + "' no esta disponible");
            }

            BigDecimal subtotal = plato.precio().multiply(BigDecimal.valueOf(item.cantidad()));

            ItemPedido linea = new ItemPedido();
            linea.setPlatoId(plato.id());
            linea.setNombrePlato(plato.nombre());
            linea.setPrecioUnitario(plato.precio());
            linea.setCantidad(item.cantidad());
            linea.setSubtotal(subtotal);
            pedido.getItems().add(linea);

            total = total.add(subtotal);
        }

        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.CREADO);
        Pedido guardado = pedidoRepository.save(pedido);

        publicarEvento(guardado);
        return guardado;
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public List<Pedido> listarPorCliente(String email) {
        return pedidoRepository.findByClienteEmailOrderByFechaCreacionDesc(email);
    }

    public Pedido obtener(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    // Cambia el estado (lo usa cocina al avanzar la preparacion).
    public Pedido cambiarEstado(Long id, EstadoPedido nuevo) {
        Pedido pedido = obtener(id);
        pedido.setEstado(nuevo);
        return pedidoRepository.save(pedido);
    }

    private void publicarEvento(Pedido pedido) {
        List<OrderEvent.Linea> lineas = pedido.getItems().stream()
                .map(i -> new OrderEvent.Linea(i.getPlatoId(), i.getCantidad()))
                .toList();
        orderPublisher.publicarPedidoCreado(
                new OrderEvent(pedido.getId(), pedido.getClienteEmail(), pedido.getTotal(), lineas));
    }
}
