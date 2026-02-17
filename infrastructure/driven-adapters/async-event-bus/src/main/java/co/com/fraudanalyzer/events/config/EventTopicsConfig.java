package co.com.fraudanalyzer.events.config;

import co.com.fraudanalyzer.model.events.config.EventTopics;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EventTopicsConfig implements EventTopics {
    @Value("${events.topics.fraud-result}")
    private String fraudResult;
}
