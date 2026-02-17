# Async Event Bus - Kafka Gateway

Gateway genérico para publicación de eventos en Kafka usando ReactiveCommons.

## Características

- ✅ Gateway genérico reutilizable para cualquier tipo de evento
- ✅ Configuración de topics por ambiente (application.yaml)
- ✅ Retry automático con backoff exponencial
- ✅ Logging de eventos y errores
- ✅ Totalmente reactivo (Project Reactor)

## Configuración

### application.yaml

```yaml
events:
  topics:
    transaction-received: "shieldflow.transaction.received"
    # Agrega más topics según necesites
  retry:
    max-attempts: 3
    delay-seconds: 2

reactive:
  commons:
    kafka:
      app:
        connectionProperties:
          bootstrap-servers: "localhost:9092"
```

## Uso

### 1. Crear tu Use Case (sin dependencias de Spring)

```java
@RequiredArgsConstructor
public class TransactionUseCase {
    
    private final EventGateway<Transaction> eventGateway;
    private final String transactionTopic;  // Inyectado como String simple
    
    public Mono<Void> processTransaction(Transaction transaction) {
        return eventGateway.emit(transactionTopic, transaction.getId(), transaction);
    }
}
```

### 2. Configurar el bean en la capa de aplicación

```java
@Configuration
public class UseCasesConfig {
    
    @Bean
    public TransactionUseCase transactionUseCase(
            EventGateway<Transaction> eventGateway,
            @Value("${events.topics.transaction-received}") String topic) {
        return new TransactionUseCase(eventGateway, topic);
    }
}
```

### 3. Para otros tipos de eventos

```java
// UseCase (dominio - sin Spring)
@RequiredArgsConstructor
public class OrderUseCase {
    
    private final EventGateway<Order> eventGateway;
    private final String orderTopic;
    
    public Mono<Void> createOrder(Order order) {
        return eventGateway.emit(orderTopic, order.getId(), order);
    }
}

// Config (aplicación - con Spring)
@Bean
public OrderUseCase orderUseCase(
        EventGateway<Order> eventGateway,
        @Value("${events.topics.order-created}") String topic) {
    return new OrderUseCase(eventGateway, topic);
}
```

## Manejo de Errores

El gateway incluye:
- **Retry automático**: 3 intentos por defecto con delay de 2 segundos
- **Logging**: Registra intentos de retry y errores finales
- **Backoff exponencial**: El delay aumenta entre reintentos

## Testing

Ver `ReactiveEventsGatewayTest.java` para ejemplos de cómo testear el gateway.

## Dependencias

```gradle
implementation 'org.reactivecommons:async-kafka-starter:7.0.1'
implementation 'io.projectreactor.kafka:reactor-kafka'
```
