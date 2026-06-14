package order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Pedidos de un cliente, mas recientes primero.
    List<Pedido> findByClienteEmailOrderByFechaCreacionDesc(String clienteEmail);
}
