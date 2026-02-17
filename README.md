# 🔍 FraudAnalyzer - Microservicio de Análisis de Fraude

Microservicio reactivo para análisis de transacciones financieras. Implementa patrones de resiliencia para garantizar procesamiento confiable y cero pérdida de mensajes.

## 📋 Descripción

FraudAnalyzer consume eventos de transacciones desde Kafka, analiza montos sospechosos, implementa reintentos automáticos ante fallos y envía mensajes fallidos a Dead Letter Queue (DLQ).

## 🔒 Características de Resiliencia

- ✅ **Reintentos automáticos** (3 intentos con 1s delay)
- ✅ **Dead Letter Queue (DLQ)** (Cero pérdida de mensajes)
- ✅ **Idempotencia** (Prevención de duplicados)
- ✅ **Análisis de montos** (Rechaza > $10,000)

## 🏗️ Arquitectura

```
Kafka Consumer ← transaction.received
    ↓
EventHandler (Retry Logic)
    ↓
UseCase (Fraud Analysis)
    ↓
Repository (R2DBC)
    ↓
PostgreSQL (5433)
    ↓
Kafka Producer → fraudanalyzer.result
    ↓
DLQ (on failure) → transaction.received.dlq
```

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 17+
- Gradle 7.x+
- PostgreSQL (puerto 5433)
- Kafka (puerto 9092)

### Configuración

**application.yaml**
```yaml
server:
  port: 8081

spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5433/fraudanalyzer_db
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: "localhost:9092"

dlq:
  topic: "shieldflow.transaction.received.dlq"

events:
  topics:
    fraud-result: "fraudanalyzer.result"
```

### Ejecutar

```bash
./gradlew bootRun
```

## 📡 Eventos Kafka

### Consume: `shieldflow.transaction.received`
```json
{
  "name": "shieldflow.transaction.received",
  "eventId": "tx-001",
  "data": {
    "id": "tx-001",
    "accountId": "ACC123",
    "amount": 500.00,
    "currency": "USD",
    "status": "PENDING"
  }
}
```

### Produce: `fraudanalyzer.result`
```json
{
  "name": "fraudanalyzer.result",
  "eventId": "tx-001",
  "data": {
    "id": "tx-001",
    "accountId": "ACC123",
    "amount": 500.00,
    "currency": "USD",
    "status": "APPROVED"
  }
}
```

### DLQ: `shieldflow.transaction.received.dlq`
Mensajes que fallaron después de 3 reintentos.

## 🧪 Pruebas

### Análisis Normal
```bash
# Desde ShieldFlow, enviar transacción normal
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"id":"test-001","accountId":"ACC123","amount":500,"currency":"USD"}'
```

**Logs esperados:**
```
INFO: Analyzing transaction: test-001
INFO: Transaction approved: test-001
```

### Análisis con Rechazo
```bash
# Monto > $10,000
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"id":"test-002","accountId":"ACC123","amount":15000,"currency":"USD"}'
```

**Logs esperados:**
```
INFO: Analyzing transaction: test-002
WARN: Transaction rejected: test-002 (Amount exceeds limit)
```

### Resiliencia y DLQ
```bash
# Chaos Mode: amount=9999 simula fallo de DB
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"id":"dlq-test","accountId":"ACC","amount":9999,"currency":"USD"}'
```

**Logs esperados:**
```
WARN: CHAOS MODE: Simulating DB failure for amount=9999
WARN: Retry attempt 1 for transaction: dlq-test
WARN: Retry attempt 2 for transaction: dlq-test
WARN: Retry attempt 3 for transaction: dlq-test
SEVERE: Max retries exhausted for: dlq-test
INFO: ✅ Message sent to DLQ: dlq-test
```

**Verificar DLQ:**
```bash
docker exec -it deployment-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic shieldflow.transaction.received.dlq \
  --from-beginning
```

## 🔗 Integración con ShieldFlow

FraudAnalyzer consume eventos del topic `shieldflow.transaction.received` publicados por **ShieldFlow**.

**Repositorio ShieldFlow:** [[Link al repo](https://github.com/ManuelCarrascalPragma/reactive-event-driven-scaffold/tree/develop/shieldFlow)]

**Topics Kafka:**
- **Consume:** `shieldflow.transaction.received`
- **Produce:** `fraudanalyzer.result`
- **DLQ:** `shieldflow.transaction.received.dlq`

## 🛠️ Tecnologías

- Spring Boot 3.x
- Spring WebFlux
- R2DBC PostgreSQL
- Reactive Commons (Kafka)
- Spring Kafka (DLQ)
- Lombok

## 📊 Estructura del Proyecto

```
domain/
  ├── model/          # Entidades de dominio
  └── usecase/        # Lógica de negocio
infrastructure/
  ├── driven-adapters/
  │   ├── r2dbc-postgresql/    # Persistencia
  │   └── async-event-bus/     # Kafka producer
  └── entry-points/
      └── async-event-handler/ # Kafka consumer + DLQ
applications/
  └── app-service/    # Configuración principal
```

## 🔧 Configuración Avanzada

### Reintentos
- **Intentos:** 3
- **Delay:** 1 segundo entre intentos
- **Configuración en:** `EventsHandler.java`

### DLQ
- **Topic:** `shieldflow.transaction.received.dlq`
- **Serializer:** `JacksonJsonSerializer` (Spring Kafka 4.0+)
- **Configuración en:** `ResilienceConfig.java`

### Chaos Mode (Testing)
- **Trigger:** `amount = 9999`
- **Comportamiento:** Simula fallo de DB
- **Uso:** Solo para pruebas de resiliencia

## 📝 Logs

```
INFO: Analyzing transaction: tx-001
INFO: Transaction approved: tx-001
WARN: Retry attempt 1 for transaction: tx-002
INFO: ✅ Message sent to DLQ: tx-003
```

## 🐛 Troubleshooting

### Error: "Connection refused to PostgreSQL"
```bash
docker ps | grep postgres-fraudanalyzer
docker logs postgres-fraudanalyzer
```

### Error: "Kafka consumer not receiving messages"
```bash
docker exec -it deployment-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group fraud-analyzer-group
```

### Ver mensajes en DLQ
```bash
docker exec -it deployment-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic shieldflow.transaction.received.dlq \
  --from-beginning
```

## 📄 Licencia

MIT License


Esto levantará:
- **Kafka** en `localhost:9092`
- **PostgreSQL ShieldFlow** en `localhost:5432` (base de datos: `shieldflow_db`)
- **PostgreSQL FraudAnalyzer** en `localhost:5433` (base de datos: `fraudanalyzer_db`)

Las tablas se crean automáticamente al iniciar los contenedores.

### Ejecutar la aplicación

```bash
./gradlew bootRun
```

La aplicación estará disponible en `http://localhost:8081`

# Arquitectura

![Clean Architecture](https://miro.medium.com/max/1400/1*ZdlHz8B0-qu9Y-QO3AXR_w.png)

## Domain

Es el módulo más interno de la arquitectura, pertenece a la capa del dominio y encapsula la lógica y reglas del negocio mediante modelos y entidades del dominio.

## Usecases

Este módulo gradle perteneciente a la capa del dominio, implementa los casos de uso del sistema, define lógica de aplicación y reacciona a las invocaciones desde el módulo de entry points, orquestando los flujos hacia el módulo de entities.

## Infrastructure

### Helpers

En el apartado de helpers tendremos utilidades generales para los Driven Adapters y Entry Points.

Estas utilidades no están arraigadas a objetos concretos, se realiza el uso de generics para modelar comportamientos
genéricos de los diferentes objetos de persistencia que puedan existir, este tipo de implementaciones se realizan
basadas en el patrón de diseño [Unit of Work y Repository](https://medium.com/@krzychukosobudzki/repository-design-pattern-bc490b256006)

Estas clases no puede existir solas y debe heredarse su compartimiento en los **Driven Adapters**

### Driven Adapters

Los driven adapter representan implementaciones externas a nuestro sistema, como lo son conexiones a servicios rest,
soap, bases de datos, lectura de archivos planos, y en concreto cualquier origen y fuente de datos con la que debamos
interactuar.

### Entry Points

Los entry points representan los puntos de entrada de la aplicación o el inicio de los flujos de negocio.

## Application

Este módulo es el más externo de la arquitectura, es el encargado de ensamblar los distintos módulos, resolver las dependencias y crear los beans de los casos de use (UseCases) de forma automática, inyectando en éstos instancias concretas de las dependencias declaradas. Además inicia la aplicación (es el único módulo del proyecto donde encontraremos la función “public static void main(String[] args)”.

**Los beans de los casos de uso se disponibilizan automaticamente gracias a un '@ComponentScan' ubicado en esta capa.**
