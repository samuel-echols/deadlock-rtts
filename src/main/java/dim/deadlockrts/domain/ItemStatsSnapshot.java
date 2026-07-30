package dim.deadlockrts.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "item_stats_snapshot")
public class ItemStatsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "patch_id", nullable = false)
    private Integer patchId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "rank_bucket", nullable = false)
    private Short rankBucket;

    @Column(name = "matches", nullable = false)
    private Integer matches;

    @Column(name = "wins", nullable = false)
    private Integer wins;

    @Column(name = "losses", nullable = false)
    private Integer losses;

    @Column(name = "players", nullable = false)
    private Integer players;

    @Column(name = "win_rate", nullable = false, precision = 8, scale = 5)
    private BigDecimal winRate;

    @Column(name = "avg_buy_time_s", precision = 10, scale = 3)
    private BigDecimal avgBuyTimeS;

    protected ItemStatsSnapshot() {}

    public ItemStatsSnapshot(LocalDate snapshotDate, Integer patchId, Integer itemId,
                              Short rankBucket, Integer matches, Integer wins, Integer losses,
                              Integer players, BigDecimal winRate, BigDecimal avgBuyTimeS) {
        this.snapshotDate = snapshotDate;
        this.patchId = patchId;
        this.itemId = itemId;
        this.rankBucket = rankBucket;
        this.matches = matches;
        this.wins = wins;
        this.losses = losses;
        this.players = players;
        this.winRate = winRate;
        this.avgBuyTimeS = avgBuyTimeS;
    }

    public Long getId() { return id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public Integer getPatchId() { return patchId; }
    public Integer getItemId() { return itemId; }
    public Short getRankBucket() { return rankBucket; }
    public Integer getMatches() { return matches; }
    public Integer getWins() { return wins; }
    public Integer getLosses() { return losses; }
    public Integer getPlayers() { return players; }
    public BigDecimal getWinRate() { return winRate; }
    public BigDecimal getAvgBuyTimeS() { return avgBuyTimeS; }
}
