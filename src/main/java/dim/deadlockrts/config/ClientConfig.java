package dim.deadlockrts.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DeadlockApiProperties.class)
public class ClientConfig {

    @Bean
    RestClient deadlockApiClient(RestClient.Builder builder, DeadlockApiProperties props) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .build();
    }
}
