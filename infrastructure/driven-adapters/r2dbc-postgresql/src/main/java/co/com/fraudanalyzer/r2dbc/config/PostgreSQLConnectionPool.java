package co.com.fraudanalyzer.r2dbc.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PostgreSQLConnectionPool {

	@Bean
	@Primary
	public ConnectionFactory connectionFactory(R2dbcProperties properties) {
		return ConnectionFactoryBuilder
				.withUrl(properties.getUrl())
				.username(properties.getUsername())
				.password(properties.getPassword())
				.build();
	}
}
