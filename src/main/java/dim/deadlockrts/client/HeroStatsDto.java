package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeroStatsDto(
        @JsonProperty("hero_id")  Integer heroId,
        @JsonProperty("bucket")   Integer bucket,
        @JsonProperty("wins")     Integer wins,
        @JsonProperty("losses")   Integer losses,
        @JsonProperty("matches")  Integer matches
) {}
