# Levantar Hamburguesas Toby (versión simplificada)

> Proyecto recortado a **3 microservicios** (menu, order, report) + Eureka + Gateway + Frontend,
> con **replicación Cassandra de 3 nodos en una sola PC**.

## Requisitos previos
- Docker + Docker Compose
- Java 21
- Maven (binario local: `../.tools/apache-maven-3.9.9/bin/mvn`)

---

## Paso 1 — Iniciar Docker

```bash
sudo systemctl start docker
docker ps   # verificar
```

---

## Paso 2 — Levantar infraestructura base (Postgres + RabbitMQ)

Desde la raíz del proyecto (`hamburguesas-toby/`):

```bash
docker compose up -d
```

| Servicio    | Puerto host | URL / Acceso              | Credenciales  |
|-------------|-------------|---------------------------|---------------|
| PostgreSQL  | 5433        | `localhost:5433`          | toby / toby   |
| RabbitMQ    | 5672        | `localhost:5672` (AMQP)   | toby / toby   |
| RabbitMQ UI | 15672       | http://localhost:15672    | toby / toby   |

Bases de datos creadas automáticamente: `menudb` (menu-service), `orderdb` (order-service).

```bash
docker compose ps   # verificar UP
```

---

## Paso 2.5 — Cassandra: cluster de 3 nodos en 1 PC (DB de report-service)

report-service usa Cassandra con **replicación de 3 nodos**, todos en esta misma PC.
Los 3 contenedores se unen en un solo cluster por gossip a través de una red bridge interna.
Solo el nodo 1 expone `:9042` al host; el driver descubre los otros 2 por gossip.

### Levantar el cluster

```bash
docker compose -f docker-compose.cassandra.yml up -d
```

> Arranca en orden: cada nodo espera (`healthcheck`) a que el anterior esté `UN` antes
> de unirse. Tarda ~2-3 min en total.

### Esperar a que los 3 nodos estén UN

```bash
docker exec -it toby-cassandra1 nodetool status
# Deben aparecer 3 líneas "UN" (Up/Normal).
```

### Crear el esquema — UNA sola vez (cuando los 3 estén UN)

```bash
docker exec -i toby-cassandra1 cqlsh -f /init-cassandra.cql
```

Crea el keyspace `reportks` con `replication_factor: 3`, el UDT `item_venta` y las
tablas `ventas` y `alertas_stock`.

### Demostrar la replicación

```bash
# ¿En qué nodos vive una venta? Con RF=3 debe listar las 3 IPs.
docker exec -it toby-cassandra1 nodetool getendpoints reportks ventas <pedidoId>

# Tolerancia a fallos: apaga un nodo y los reportes siguen respondiendo.
docker compose -f docker-compose.cassandra.yml stop cassandra3
```

> Si un nodo falla al unirse con `Bootstrap Token collision` (colisión aleatoria de tokens),
> recréalo limpio:
> ```bash
> docker rm -f toby-cassandra3
> docker volume rm hamburguesas-toby_cassandra3-data
> docker compose -f docker-compose.cassandra.yml up -d cassandra3
> ```

---

## Paso 3 — Levantar microservicios (el orden importa)

Abrir una terminal por servicio, o usar el dashboard de Spring Boot en VS Code.
Maven local: `MVN=../.tools/apache-maven-3.9.9/bin/mvn`

### 3.1 Eureka Server (registro de servicios)

```bash
$MVN -pl eureka-server spring-boot:run
```
**URL:** http://localhost:8761 — esperar `Started Eureka Server`.

### 3.2 Menu Service (platos y combos)

```bash
$MVN -pl menu-service spring-boot:run
```
**Puerto:** 8082 · **DB:** PostgreSQL `menudb` · **Rutas:** `/menu/**`

### 3.3 Order Service (pedidos)

```bash
$MVN -pl order-service spring-boot:run
```
**Puerto:** 8083 · **DB:** PostgreSQL `orderdb` · **Rutas:** `/pedidos/**`
Valida platos contra menu-service (`lb://menu-service`) y publica `order.created` a RabbitMQ.

### 3.4 Report Service (reportes y analítica)

```bash
$MVN -pl report-service spring-boot:run
```
**Puerto:** 8087 · **DB:** Cassandra `reportks` (3 nodos, ver Paso 2.5) · **Rutas:** `/reportes/**`
Consume `order.created` y agrega datos para reportes.
> Arrancarlo **antes** de generar pedidos si quieres que los capte: el exchange no reenvía eventos pasados.
> Requiere Cassandra arriba y el esquema creado (Paso 2.5).

### 3.5 API Gateway (entrada única)

```bash
$MVN -pl api-gateway spring-boot:run
```
**URL:** http://localhost:8080 — arrancar **al final**.

---

## Paso 4 — Frontend (React + Vite)

```bash
cd frontend
npm install      # solo la primera vez
npm run dev
```

**URL:** http://localhost:5173 — **sin login** (user-service eliminado), abre directo en Menú.
Pedidos usan cliente fijo `cliente@toby.com`. Pantallas: **Menú** (arma carrito → crea pedido)
y **Reportes** (ventas, platos más vendidos).
Llama al gateway vía proxy `/api` (`vite.config.js`), sin CORS.

---

## Resumen de puertos

| Servicio       | Puerto | URL                        |
|----------------|--------|----------------------------|
| API Gateway    | 8080   | http://localhost:8080      |
| Menu Service   | 8082   | http://localhost:8082      |
| Order Service  | 8083   | http://localhost:8083      |
| Report Service | 8087   | http://localhost:8087      |
| Frontend (Vite)| 5173   | http://localhost:5173      |
| Eureka Server  | 8761   | http://localhost:8761      |
| PostgreSQL     | 5433   | localhost:5433             |
| Cassandra (n1) | 9042   | localhost:9042             |
| RabbitMQ AMQP  | 5672   | localhost:5672             |
| RabbitMQ UI    | 15672  | http://localhost:15672     |

---

## Apagar todo

```bash
docker compose stop                                  # infra base (conserva datos)
docker compose -f docker-compose.cassandra.yml stop  # cluster cassandra

# Borrar todo + datos:
docker compose down -v
docker compose -f docker-compose.cassandra.yml down -v
```
