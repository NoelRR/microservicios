package order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody PedidoRequest req) {
        return pedidoService.crearPedido(req);
    }

    // Todos (vista admin/cocina). Filtra por cliente con ?email=
    @GetMapping
    public List<Pedido> listar(@RequestParam(required = false) String email) {
        return (email != null)
                ? pedidoService.listarPorCliente(email)
                : pedidoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Pedido obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    // Avanza el estado del pedido (cocina: EN_PREPARACION, LISTO, etc).
    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable Long id, @RequestParam EstadoPedido estado) {
        return pedidoService.cambiarEstado(id, estado);
    }
}
