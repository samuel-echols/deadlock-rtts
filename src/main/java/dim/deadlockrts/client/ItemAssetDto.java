package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemAssetDto(
        @JsonProperty("id")         Long itemId,
        @JsonProperty("class_name") String className,
        @JsonProperty("name")       String displayName
) {}
