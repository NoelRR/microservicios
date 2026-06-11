package com.toby.report.repository;

import com.toby.report.model.VentaRegistro;
import org.springframework.data.cassandra.repository.CassandraRepository;

// pedidoId es la clave de particion (PK) de la tabla ventas.
public interface VentaRepository extends CassandraRepository<VentaRegistro, Long> {

    // existsById sobre la PK; equivale al existsByPedidoId anterior.
    default boolean existsByPedidoId(Long pedidoId) {
        return existsById(pedidoId);
    }
}
