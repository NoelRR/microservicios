# projectContext.md — Hamburguesas Toby (microservicios)

> Contexto para retomar el proyecto en una nueva sesión. Última actualización: 2026-06-09.

## Qué es

Sistema de microservicios para "Hamburguesas Toby" (comida rápida), basado en el PDF
`../Beneficios de la Propuesta 1.pdf`. Objetivo: resolver retrasos en hora pico, falta de
sincronización caja↔cocina, control manual de inventario, integración entre sucursales, reportes.

Proyecto académico (Distribuidos). Stack: **Spring Boot 3.3.5 + Spring Cloud 2023.0.3 + Java 21 + RabbitMQ**.
Mensajería: **RabbitMQ** (no Kafka — volumen bajo, caso = colas de tareas con routing).

## Ubicación y toolchain

- Raíz proyecto: `/home/nole/Documents/Programacion/Distribuidos/microServicios/hamburguesas-toby`
- **No hay Maven global.** Binario local: `../.tools/apache-maven-3.9.9/bin/mvn`
  (relativo a la raíz del proyecto; `.tools/` está en la carpeta padre `microServicios/`).
- Java 21 ✓, Docker ✓ (daemon necesita arranque manual: el usuario hizo
  `sudo systemctl start docker` + `sudo chmod 666 /var/run/docker.sock` porque `nole`
  no está en el grupo `docker`).

## Arquitectura (3 microservicios + infra) — SIMPLIFICADA 2026-06-09

> Proyecto recortado a 3 servicios + replicacion Cassandra de 3 nodos en 3 PCs distintas (1 por PC).
> Eliminados: user-service, kitchen-service, inventory-service, billing-service.

```
[API Gateway :8080] ──→ [Eureka :8761]
      ├─ menu-service     :8082  PostgreSQL menudb       (platos/combos)
      ├─ order-service    :8083  PostgreSQL orderdb      ─publish→ order.created (valida vs menu)
      └─ report-service   :8087  Cassandra reportks (3 nodos, RF=3)  ←consume order.created
```

Nota: report-service quedo enfocado solo en ventas. Se elimino toda la cadena de
`stock.low` (listener, cola/binding, StockEvent, AlertaStock/AlertaRepository,
endpoint /reportes/alertas-stock, tabla `alertas_stock` y seccion de alertas del
frontend) porque su productor (inventory-service) fue eliminado. menu-service ya no
consume `stock.low`. Frontend sin auth (user-service eliminado): cliente fijo
`cliente@toby.com`, sin login ni pantalla Cocina.

## Servicios activos (tras simplificación 2026-06-09)

| Servicio | Estado |
|---|---|
| eureka-server, api-gateway | ✅ activo |
| menu-service (CRUD platos/combos) | ✅ activo |
| order-service (publish order.created) | ✅ activo |
| report-service (consume order.created, Cassandra 3 nodos RF=3) | ✅ activo |

**Eliminados 2026-06-09:** user-service, kitchen-service, inventory-service, billing-service
(carpetas borradas, módulos quitados del pom, rutas quitadas del gateway, mongo + dbs userdb/inventorydb/billingdb quitadas del compose). Las secciones detalladas de esos servicios más abajo quedan como referencia histórica.

## Estado actual del código

Módulos en `pom.xml` (parent reactor): eureka-server, api-gateway, menu-service, order-service, report-service.
**Reactor compila: EXIT=0 (verificado 2026-06-09).** Frontend `npm run build` OK (37 módulos).

### eureka-server (:8761)
- `@EnableEurekaServer`. No se registra a sí mismo. Sin lógica.

### api-gateway (:8080)
- Spring Cloud Gateway + Eureka client. `discovery.locator` activo.
- Rutas explícitas: `/auth/**` y `/usuarios/**` → user-service; `/menu/**` → menu-service.

### user-service (:8081, PostgreSQL userdb) — VERIFICADO
- Modelo: `Usuario` (id, email único, password BCrypt, nombre, `Rol`, activo).
  `Rol` enum = ADMIN / EMPLEADO / CLIENTE.
- Seguridad JWT stateless: `JwtService` (jjwt 0.12.6, HS384 efectivo), `JwtAuthFilter`,
  `SecurityConfig` (`@EnableMethodSecurity`), `CustomUserDetailsService` (authority `ROLE_<rol>`).
- `AuthService`: register (siempre CLIENTE) + login. `UsuarioService`: alta empleado (rol explícito),
  listar, toggle activo.
- Controllers: `AuthController` `/auth/**` (público), `UsuarioController` `/usuarios/**`
  (`@PreAuthorize("hasRole('ADMIN')")`).
- `DataInitializer`: crea admin por defecto **admin@toby.com / admin123** al primer arranque.
- Secret JWT vía env `JWT_SECRET` (default en yml; cambiar en prod).

### menu-service (:8082, PostgreSQL menudb) — compila, falta runtime
- Modelo: `Plato` (con flags `activo`=admin publica/oculta, `disponible`=hay stock).
  **Combos eliminados** (Combo/ComboRepository/ComboRequest borrados, endpoints quitados).
- CRUD platos. `GET /menu/items` = solo `activo && disponible` (menú público).
- Endpoints: GET `/menu/items`, GET `/menu/items/all`, GET `/menu/items/{id}`,
  POST/PUT/DELETE `/menu/items`, PATCH `/menu/items/{id}/toggle`.
- RabbitMQ: consume exchange `inventory.exchange` (topic), routing `stock.low`,
  cola `menu.stock-low`, payload `StockEvent{platoId, disponible}` → `MenuService.marcarDisponibilidad`.
  **PENDIENTE decidir** quién mapea ingrediente→platos (inventory manda platoIds afectados,
  o agregar tabla `plato_ingredientes` en menu). Por ahora el evento ya trae platoId directo.

### order-service (:8083, PostgreSQL orderdb) — VERIFICADO
- Modelo: `Pedido` (clienteEmail, items, total, `EstadoPedido`, fechaCreacion) +
  `ItemPedido` (platoId, **snapshot** nombrePlato/precioUnitario, cantidad, subtotal).
  `EstadoPedido` enum = CREADO / EN_PREPARACION / LISTO / ENTREGADO / CANCELADO.
- `MenuClient`: RestClient `@LoadBalanced` → `http://menu-service/menu/items/{id}` para validar
  plato (activo+disponible) y congelar precio actual. Falla → `PedidoInvalidoException` (400).
- `PedidoService.crearPedido`: valida cada item, congela precios, suma total, persiste, publica evento.
- RabbitMQ publisher: exchange `orders.exchange` (topic, durable), routing `order.created`,
  payload `OrderEvent{pedidoId, clienteEmail, total, items[{platoId,cantidad}]}`.
  Lo consumirán kitchen (encolar prep) e inventory (descontar stock).
- Endpoints: POST `/pedidos`, GET `/pedidos` (+`?email=`), GET `/pedidos/{id}`,
  PATCH `/pedidos/{id}/estado?estado=`.
- Gateway: ruta `/pedidos/**` → order-service añadida.
- Prueba humo OK: pedido creado valida vía lb, total correcto, exchange creado, evento publicado,
  plato inexistente → 400, cambio de estado OK.

### kitchen-service (:8084, MongoDB kitchendb) — VERIFICADO
- Modelo: `Comanda` (doc MongoDB: pedidoId, clienteEmail, items[`ItemComanda`{platoId,cantidad}],
  `EstadoComanda`, fechaRecepcion, fechaActualizacion). Enum = RECIBIDA / EN_PREPARACION / LISTA.
- RabbitMQ consumer: cola `kitchen.orders` bindeada a `orders.exchange` routing `order.created`.
  ⚠️ Converter JSON con `TypePrecedence.INFERRED` (ignora header `__TypeId__` de order-service,
  usa el tipo del parámetro del listener) → así deserializa `OrderCreatedEvent` sin compartir clases.
- `OrderListener` → `CocinaService.registrarComanda` crea comanda RECIBIDA.
- Sync inverso: al avanzar comanda, `OrderClient` (RestClient `@LoadBalanced` →
  `http://order-service`) hace PATCH `/pedidos/{id}/estado`. Mapeo: EN_PREPARACION→EN_PREPARACION,
  LISTA→LISTO. Fallo de sync solo loguea (no bloquea cocina).
- Endpoints: GET `/cocina/comandas` (+`?estado=`), GET `/cocina/comandas/{id}`,
  PATCH `/cocina/comandas/{id}/estado?estado=`.
- Gateway: ruta `/cocina/**` → kitchen-service añadida.
- Prueba humo OK: pedido creado → cocina consume evento y crea comanda RECIBIDA →
  avanzar a EN_PREPARACION/LISTA propaga estado al pedido en order-service (verificado bidireccional).

### inventory-service (:8085, PostgreSQL inventorydb) — VERIFICADO
- Modelo: `Ingrediente` (nombre único, stock, umbralMinimo; `haySuficiente()` = stock>umbral) +
  `RecetaItem` (platoId → `@ManyToOne Ingrediente`, cantidad por plato). Receta resuelve el mapeo
  ingrediente↔platos que estaba pendiente.
- Consume `order.created` (cola `inventory.orders` ← `orders.exchange`). Por cada línea descuenta
  `recetaItem.cantidad * cantidadPedida` (clamp a 0) de cada ingrediente del plato.
- Recalcula disponibilidad de **todos** los platos que comparten ingredientes tocados
  (snapshot antes/después) y publica `StockEvent{platoId, disponible}` **solo si cambió**, a
  `inventory.exchange` routing `stock.low`. menu-service ya lo consume (cola `menu.stock-low`).
- Publicar StockEvent funciona cross-service sin clase compartida: el converter default de menu
  usa `TypePrecedence.INFERRED` (verificado en bytecode spring-amqp 3.1.7) → ignora header `__TypeId__`.
- `reabastecer` (PATCH) suma stock y, si destraba platos, publica `stock.low` con disponible=true.
- Endpoints: GET/POST `/inventario/ingredientes`, GET `/inventario/ingredientes/{id}`,
  PATCH `/inventario/ingredientes/{id}/reabastecer?cantidad=`, POST `/inventario/recetas`,
  GET `/inventario/recetas?platoId=`.
- Gateway: ruta `/inventario/**` añadida.
- Prueba humo OK (círculo completo): pedido plato3 x2 → stock pan 10→4 (≤umbral 5) →
  stock.low(false) → menu oculta plato3 de `/menu/items`. Reabastecer +10 → stock.low(true) →
  plato3 reaparece. Verificado bidireccional.

### billing-service (:8086, PostgreSQL billingdb) — VERIFICADO
- Modelo: `Factura` (numeroFactura único "FAC-<pedidoId>", pedidoId único, clienteEmail, lineas,
  subtotal/impuesto/total, `EstadoPago`, fechaEmision) + `LineaFactura` (snapshot precios).
  `EstadoPago` = PENDIENTE / PAGADA / ANULADA.
- Consume `order.created` (cola `billing.orders` ← `orders.exchange`) como disparador; el evento
  solo trae total+platoIds, así que trae el **detalle con precios** vía `OrderClient`
  (RestClient `@LoadBalanced` → `http://order-service/pedidos/{id}`).
- Factura al crear el pedido (modelo caja de comida rápida). subtotal=total del pedido (neto),
  impuesto = subtotal * `billing.iva-rate` (default 0.16, en yml), total = subtotal+impuesto.
- **Idempotente**: `pedidoId` único + `findByPedidoId` → reenvío del evento no duplica factura
  (verificado: republicado order.created, factura intacta y sigue PAGADA).
- Endpoints: GET `/facturas` (+`?email=`), GET `/facturas/{id}`, GET `/facturas/pedido/{pedidoId}`,
  PATCH `/facturas/{id}/pagar`.
- Gateway: ruta `/facturas/**` añadida.
- Prueba humo OK: pedido plato3 x1 (8.50) → factura FAC-4 emitida (subtotal 8.50, IVA 1.36,
  total 9.86) → pagar → PAGADA. Idempotencia verificada.

### report-service (:8087, Cassandra keyspace `reportks`) — SOLO VENTAS
- DB Cassandra con replicación de 3 nodos (`replication_factor: 3`). Esquema en
  `infra/init-cassandra.cql`. Cluster: `docker-compose.cassandra.yml` (3 nodos en 1 PC).
- Modelo: `VentaRegistro` (`@Table("ventas")`, PK = `pedidoId`) + `ItemVenta` (UDT `item_venta`,
  embebido en lista `items`). (AlertaStock eliminado junto a toda la cadena stock.low.)
- **Consume un exchange**: `order.created` (cola `report.orders` ← `orders.exchange`).
  Un `@RabbitListener` en `EventListener`.
- Ingesta de venta idempotente (`existsByPedidoId` — PK = upsert natural en Cassandra).
- Reportes: `resumenVentas` (Java stream), `platosMasVendidos` (**agregación en memoria Java**:
  Cassandra no soporta unwind/group; OK para volumen académico).
- Vars de entorno en arranque: `CASSANDRA_HOST`, `CASSANDRA_DC`, `RABBITMQ_HOST`, `EUREKA_URL`
  (todos con defaults a `localhost` para correr en una sola máquina sin cambios).
- Endpoints: GET `/reportes/ventas`, GET `/reportes/platos-mas-vendidos?limite=`.
- Gateway: ruta `/reportes/**` añadida.
- ⚠️ Las colas se crean al primer arranque del servicio; topic exchange NO reenvía eventos
  pasados, solo capta los nuevos desde que el servicio existe.
- ⚠️ Requiere que el esquema Cassandra esté creado antes de arrancar:
  `docker exec -i toby-cassandra1 cqlsh -f /init-cassandra.cql`

## Infraestructura (docker-compose.yml)

- postgres:16 → **host 5433** (no 5432: el host ya tiene un postgres local en 5432).
  user/pass `toby/toby`. `infra/init-databases.sql` crea `orderdb` (menudb la crea POSTGRES_DB).
- rabbitmq:3-management → 5672 (AMQP) + 15672 (consola web, toby/toby)
- ⚠️ application.yml de menu apunta a `localhost:5433` por el conflicto de puerto.
- (mongo eliminado: solo lo usaba kitchen-service.)

### Cassandra (docker-compose.cassandra.yml) — cluster distribuido (1 nodo por PC)

- cassandra:5, 1 contenedor por máquina (`toby-cassandra`) con `network_mode: host`.
  Sin host-network el gossip no cruza entre PCs distintas (NAT de Docker bloquea).
- Variables requeridas: `HOST_IP` (IP real de esta PC), `SEED_IP` (IP de PC-A siempre).
- Levantar: `HOST_IP=<ip-local> SEED_IP=<ip-A> docker compose -f docker-compose.cassandra.yml up -d`
- Esquema UNA vez desde PC-A (3 nodos UN): `docker exec -i toby-cassandra cqlsh -f /init-cassandra.cql`
- Keyspace `reportks` con `replication_factor: 3` → cada fila replicada en las 3 PCs.
- Demostrar replicación: `nodetool getendpoints reportks ventas <pedidoId>` → lista 3 IPs distintas.
- Demostrar cross-PC: insertar venta en PC-A → `cqlsh` en PC-B muestra la misma fila.
- ⚠️ Firewall: abrir puerto 7000 (gossip) y 9042 (CQL) entre las PCs.
- ⚠️ Si un nodo falla con `Bootstrap Token collision`: `docker rm -f toby-cassandra` +
  `docker volume rm hamburguesas-toby_cassandra-data` + volver a levantar con las vars.

## Cómo levantar (verificado funcionando)

```bash
cd hamburguesas-toby
MVN=../.tools/apache-maven-3.9.9/bin/mvn

docker compose up -d                                  # infra base (postgres 5433, rabbit)
docker compose -f docker-compose.cassandra.yml up -d  # cluster cassandra 3 nodos
$MVN package -DskipTests                              # build jars

# arrancar (cada uno en su terminal, o java -jar en background)
$MVN -pl eureka-server spring-boot:run
$MVN -pl menu-service  spring-boot:run
$MVN -pl order-service spring-boot:run
$MVN -pl report-service spring-boot:run
$MVN -pl api-gateway   spring-boot:run
```

Ver guía operativa completa en `levantarApp.md`.

## Prueba de humo ejecutada (user-service, 2026-06-02) — TODO OK

- `POST /auth/register` (cliente) → **201**, devuelve JWT con rol CLIENTE.
- `POST /auth/login` (admin@toby.com/admin123) → JWT rol ADMIN.
- `GET /usuarios` SIN token → **403**.
- `GET /usuarios` con token admin → **200** + lista de usuarios.

Nota: al arrancar sin eureka, user-service loguea stacktrace de eureka-client (no fatal, app levanta igual).

## Próximo paso sugerido

Las 8 fases del plan están completas. Posibles mejoras (ninguna obligatoria):
- **Seguridad end-to-end**: propagar JWT de user-service a través del gateway hacia los demás
  servicios (hoy solo user-service valida token; el resto confía en el gateway).
- **Resiliencia**: Resilience4j (circuit breaker/retry) en los RestClient `lb://` y DLQ en RabbitMQ.
- **Mapeo real ingrediente→plato** en datos de demo (seed de ingredientes/recetas).
- **Tests**: de integración con Testcontainers (postgres/mongo/rabbit).
- **Reportes por periodo**: filtros `?desde=&hasta=` en report-service.

### Frontend (React + Vite) — ✅ HECHO + VERIFICADO (2026-06-07)
- Carpeta `frontend/`. Vite 5 + React 18 + react-router-dom 6. Dev en `:5173`.
- Llama al gateway vía **proxy `/api` → http://localhost:8080** (`vite.config.js`, rewrite quita
  `/api`) → sin CORS y sin tocar el backend. Cliente en `src/api.js` adjunta `Bearer <JWT>`.
- Auth en `src/auth.jsx` (Context + localStorage). Rutas privadas con guard.
- Pantallas: **Login**, **Menú** (carrito → POST /pedidos), **Cocina** (GET comandas + PATCH estado,
  refresco c/4s), **Reportes** (ventas, platos-mas-vendidos, alertas-stock).
- `npm run build` OK (40 módulos). Proxy verificado: login/menu/reportes responden vía :5173.
- Pendiente opcional: CORS real en gateway para servir el build en prod (hoy solo proxy de dev).

## Convenciones del código

- **Estructura plana**: 1 paquete corto por servicio (`menu`, `order`, `report`, `gateway`,
  `eureka`), todos los `.java` en una sola carpeta `src/main/java/<pkg>/` (sin subcarpetas de
  capas ni prefijo `com.toby`). Simplificado para proyecto académico.
- DTOs como `record` con validación jakarta. Excepciones con `@ResponseStatus`.
- Comentarios en español, concisos, explican el "por qué".
- Cada servicio = su propia BD (database-per-service).
