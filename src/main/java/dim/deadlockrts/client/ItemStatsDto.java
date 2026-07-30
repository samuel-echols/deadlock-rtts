package dim.deadlockrts.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemStatsDto(
        @JsonProperty("item_id")       Integer itemId,
        @JsonProperty("bucket")        Integer bucket,
        @JsonProperty("wins")          Integer wins,
        @JsonProperty("losses")        Integer losses,
        @JsonProperty("matches")       Integer matches,
        @JsonProperty("players")       Integer players,
        @JsonProperty("avg_buy_time_s") Double avgBuyTimeS
) {}
