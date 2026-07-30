package dim.deadlockrts.client;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class DeadlockAssetsClient {

    private static final String GAME_MODE = "normal";

    private final RestClient client;

    public DeadlockAssetsClient(RestClient deadlockApiClient) {
        this.client = deadlockApiClient;
    }

    @Retry(name = "deadlockApi")
    @RateLimiter(name = "deadlockApi")
    public List<HeroAssetDto> fetchHeroes() {
        return client.get()
                .uri("/v1/assets/heroes?language=english")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "deadlockApi")
    @RateLimiter(name = "deadlockApi")
    public List<ItemAssetDto> fetchItems() {
        return client.get()
                .uri("/v1/assets/items?language=english")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "deadlockApi")
    @RateLimiter(name = "deadlockApi")
    public List<Integer> fetchClientVersions() {
        return client.get()
                .uri("/v1/assets/client-versions")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "deadlockApi")
    @RateLimiter(name = "deadlockApi")
    public List<HeroStatsDto> fetchHeroStats(long minTimestamp, long maxTimestamp) {
        return client.get()
                .uri(u -> u.path("/v1/analytics/hero-stats")
                        .queryParam("game_mode", GAME_MODE)
                        .queryParam("min_unix_timestamp", minTimestamp)
                        .queryParam("max_unix_timestamp", maxTimestamp)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Retry(name = "deadlockApi")
    @RateLimiter(name = "deadlockApi")
    public List<ItemStatsDto> fetchItemStats(long minTimestamp, long maxTimestamp) {
        return client.get()
                .uri(u -> u.path("/v1/analytics/item-stats")
                        .queryParam("game_mode", GAME_MODE)
                        .queryParam("min_unix_timestamp", minTimestamp)
                        .queryParam("max_unix_timestamp", maxTimestamp)
                        .queryParam("min_matches", 10)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
