package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeroAssetDto(
        @JsonProperty("hero_id")    Integer heroId,
        @JsonProperty("class_name") String className,
        @JsonProperty("display_name") String displayName
) {}
