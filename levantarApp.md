# Levantar Hamburguesas Toby (versión simplificada)

> Proyecto recortado a **3 microservicios** (menu, order, report) + Eureka + Gateway + Frontend,
> con **replicación Cassandra de 3 nodos distribuidos en 3 PCs distintas (RF=3)**.

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

## Paso 2.5 — Cassandra: cluster distribuido (1 nodo por PC)

report-service usa Cassandra con **replicación RF=3** entre 3 máquinas distintas (PC-A, PC-B, PC-C).
Cada PC corre 1 nodo con `network_mode: host` — el nodo usa la IP real del host para que el
gossip cruce la LAN. Datos insertados desde cualquier PC se replican automáticamente a las otras dos.

### Prerequisito — abrir puertos en el firewall de cada PC

```bash
# Gossip inter-nodo (Cassandra se comunica entre nodos por este puerto)
sudo ufw allow 7000/tcp
# CQL (clientes y cqlsh)
sudo ufw allow 9042/tcp
```

### Levantar el nodo en cada PC

Reemplazar `192.168.X.A`, `192.168.X.B`, `192.168.X.C` con las IPs reales de la LAN.

```bash
# PC-A (seed del cluster — levantar primero)
HOST_IP=192.168.X.A SEED_IP=192.168.X.A docker compose -f docker-compose.cassandra.yml up -d

# PC-B (esperar ~60s a que PC-A esté UN, luego)
HOST_IP=192.168.X.B SEED_IP=192.168.X.A docker compose -f docker-compose.cassandra.yml up -d

# PC-C (esperar ~60s a que PC-B esté UN, luego)
HOST_IP=192.168.X.C SEED_IP=192.168.X.A docker compose -f docker-compose.cassandra.yml up -d
```

### Esperar a que los 3 nodos estén UN

```bash
# Ejecutar desde cualquier PC — deben aparecer 3 líneas "UN" con 3 IPs distintas
docker exec -it toby-cassandra nodetool status
```

Ejemplo de salida esperada:
```
Datacenter: datacenter1
=======================
Status=Up/Normal  ...
--  Address        Load    ...  State
UN  192.168.X.A   ...          Normal
UN  192.168.X.B   ...          Normal
UN  192.168.X.C   ...          Normal
```

### Crear el esquema — UNA sola vez desde PC-A (cuando los 3 estén UN)

```bash
docker exec -i toby-cassandra cqlsh -f /init-cassandra.cql
```

### Demostrar la replicación

```bash
# ¿En qué nodos vive una venta? Con RF=3 debe listar las 3 IPs distintas.
docker exec -it toby-cassandra nodetool getendpoints reportks ventas <pedidoId>

# Insertar desde PC-A y verificar en PC-B (leer directo desde su nodo):
#   En PC-B:
docker exec -it toby-cassandra cqlsh -e "SELECT * FROM reportks.ventas;"

# Tolerancia a fallos: apagar un nodo y los reportes siguen respondiendo.
docker compose -f docker-compose.cassandra.yml stop   # en PC-C, por ejemplo
```

> **Si un nodo falla al unirse** (`Bootstrap Token collision`): borrarlo limpio y recrear.
> ```bash
> docker rm -f toby-cassandra
> docker volume rm hamburguesas-toby_cassandra-data
> HOST_IP=192.168.X.? SEED_IP=192.168.X.A docker compose -f docker-compose.cassandra.yml up -d
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
| Cassandra      | 9042   | localhost:9042 (nodo local)|
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
