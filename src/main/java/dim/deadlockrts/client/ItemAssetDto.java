package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemAssetDto(
        @JsonProperty("item_id")    Integer itemId,
        @JsonProperty("class_name") String className,
        @JsonProperty("display_name") String displayName
) {}
