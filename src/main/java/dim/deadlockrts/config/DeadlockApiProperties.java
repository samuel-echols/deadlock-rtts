package dim.deadlockrts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deadlock-api")
public record DeadlockApiProperties(
        String baseUrl,
        String userAgent,
        String apiKey
) {}
