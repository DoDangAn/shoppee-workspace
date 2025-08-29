package com.andd.DoDangAn.DoDangAn.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;

import java.time.Duration;

@Configuration
@EnableR2dbcRepositories(
        basePackages = "com.andd.DoDangAn.DoDangAn.repository.r2dbc"
)
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "mysql")
                .option(ConnectionFactoryOptions.HOST, "localhost")
                .option(ConnectionFactoryOptions.PORT, 3306)
                .option(ConnectionFactoryOptions.DATABASE, "ecommerce")
                .option(ConnectionFactoryOptions.USER, "root")
                .option(ConnectionFactoryOptions.PASSWORD, "")
                .build();

        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxSize(10)
                .initialSize(5)
                .maxIdleTime(Duration.ofMinutes(30))
                .build();
        return new ConnectionPool(poolConfig);
    }
}