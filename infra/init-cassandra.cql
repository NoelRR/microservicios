-- Esquema de Cassandra para report-service.
-- Ejecutar UNA vez, cuando los 3 nodos esten "UN" (nodetool status):
--   docker exec -i toby-cassandra1 cqlsh -f /init-cassandra.cql
--
-- replication_factor = 3 => cada fila vive en los 3 nodos del cluster.
-- Cada dato esta replicado en los 3 nodos: si uno (o dos) se caen, los
-- reportes siguen respondiendo desde los demas. AQUI esta la replicacion
-- del proyecto. Demostrar: nodetool getendpoints reportks ventas <pedidoId>.

CREATE KEYSPACE IF NOT EXISTS reportks
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};

USE reportks;

-- UDT embebido en la lista items de ventas (mapea a com.toby.report.model.ItemVenta).
CREATE TYPE IF NOT EXISTS item_venta (
  platoid  bigint,
  cantidad int
);

-- Una fila por pedido. pedidoid = clave de particion (reparte filas entre nodos).
CREATE TABLE IF NOT EXISTS ventas (
  pedidoid     bigint PRIMARY KEY,
  clienteemail text,
  total        decimal,
  items        list<frozen<item_venta>>,
  fecha        timestamp
);
