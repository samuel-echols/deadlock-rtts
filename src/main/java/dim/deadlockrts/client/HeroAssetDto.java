package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeroAssetDto(
        @JsonProperty("id")         Integer heroId,
        @JsonProperty("class_name") String className,
        @JsonProperty("name")       String displayName
) {}
