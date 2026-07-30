package dim.deadlockrts.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hero_stats_snapshot")
public class HeroStatsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "patch_id", nullable = false)
    private Integer patchId;

    @Column(name = "hero_id", nullable = false)
    private Integer heroId;

    @Column(name = "rank_bucket", nullable = false)
    private Short rankBucket;

    @Column(name = "matches", nullable = false)
    private Integer matches;

    @Column(name = "wins", nullable = false)
    private Integer wins;

    @Column(name = "losses", nullable = false)
    private Integer losses;

    @Column(name = "win_rate", nullable = false, precision = 8, scale = 5)
    private BigDecimal winRate;

    protected HeroStatsSnapshot() {}

    public HeroStatsSnapshot(LocalDate snapshotDate, Integer patchId, Integer heroId,
                              Short rankBucket, Integer matches, Integer wins, Integer losses,
                              BigDecimal winRate) {
        this.snapshotDate = snapshotDate;
        this.patchId = patchId;
        this.heroId = heroId;
        this.rankBucket = rankBucket;
        this.matches = matches;
        this.wins = wins;
        this.losses = losses;
        this.winRate = winRate;
    }

    public Long getId() { return id; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public Integer getPatchId() { return patchId; }
    public Integer getHeroId() { return heroId; }
    public Short getRankBucket() { return rankBucket; }
    public Integer getMatches() { return matches; }
    public Integer getWins() { return wins; }
    public Integer getLosses() { return losses; }
    public BigDecimal getWinRate() { return winRate; }
}
