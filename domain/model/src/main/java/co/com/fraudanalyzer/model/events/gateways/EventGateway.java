package co.com.fraudanalyzer.model.events.gateways;

import reactor.core.publisher.Mono;

public interface EventGateway<T> {
    Mono<Void> emit(String topic, String key, T event);
}
