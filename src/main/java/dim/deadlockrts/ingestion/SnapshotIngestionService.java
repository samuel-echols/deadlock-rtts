package dim.deadlockrts.ingestion;

import dim.deadlockrts.client.DeadlockAssetsClient;
import dim.deadlockrts.client.HeroStatsDto;
import dim.deadlockrts.client.ItemStatsDto;
import dim.deadlockrts.domain.Patch;
import dim.deadlockrts.domain.repository.PatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class SnapshotIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotIngestionService.class);

    private final DeadlockAssetsClient apiClient;
    private final PatchRepository patchRepository;
    private final JdbcTemplate jdbc;

    public SnapshotIngestionService(DeadlockAssetsClient apiClient,
                                    PatchRepository patchRepository,
                                    JdbcTemplate jdbc) {
        this.apiClient = apiClient;
        this.patchRepository = patchRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public void ingest() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long dayStart = today.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        log.info("Starting snapshot ingestion for {}", today);

        int patchId = resolveCurrentPatch();
        log.info("Current patch_id={}", patchId);

        ingestHeroStats(today, patchId, dayStart, dayEnd);
        ingestItemStats(today, patchId, dayStart, dayEnd);

        log.info("Snapshot ingestion complete for {}", today);
    }

    private int resolveCurrentPatch() {
        List<Integer> versions = apiClient.fetchClientVersions();
        if (versions == null || versions.isEmpty()) {
            throw new IllegalStateException("No client versions returned from API");
        }
        int latestBuild = versions.get(versions.size() - 1);
        return patchRepository.findByBuildNumber(latestBuild)
                .map(Patch::getPatchId)
                .orElseGet(() -> {
                    Patch patch = patchRepository.save(new Patch(latestBuild));
                    log.info("Registered new patch build_number={} patch_id={}", latestBuild, patch.getPatchId());
                    return patch.getPatchId();
                });
    }

    private void ingestHeroStats(LocalDate date, int patchId, long minTs, long maxTs) {
        List<HeroStatsDto> stats = apiClient.fetchHeroStats(minTs, maxTs);
        int upserted = 0;
        for (HeroStatsDto dto : stats) {
            if (dto.heroId() == null || dto.matches() == null || dto.matches() == 0) continue;
            BigDecimal winRate = BigDecimal.valueOf(dto.wins())
                    .divide(BigDecimal.valueOf(dto.matches()), 5, RoundingMode.HALF_UP);
            short bucket = dto.bucket() == null ? 0 : dto.bucket().shortValue();
            jdbc.update("""
                    INSERT INTO hero_stats_snapshot
                        (snapshot_date, patch_id, hero_id, rank_bucket, matches, wins, losses, win_rate)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (snapshot_date, patch_id, hero_id, rank_bucket)
                    DO UPDATE SET
                        matches  = EXCLUDED.matches,
                        wins     = EXCLUDED.wins,
                        losses   = EXCLUDED.losses,
                        win_rate = EXCLUDED.win_rate
                    """,
                    date, patchId, dto.heroId(), bucket,
                    dto.matches(), dto.wins(), dto.losses(), winRate);
            upserted++;
        }
        log.info("Hero stats upserted: {} rows for {}", upserted, date);
    }

    private void ingestItemStats(LocalDate date, int patchId, long minTs, long maxTs) {
        List<ItemStatsDto> stats = apiClient.fetchItemStats(minTs, maxTs);
        int upserted = 0;
        for (ItemStatsDto dto : stats) {
            if (dto.itemId() == null || dto.matches() == null || dto.matches() == 0) continue;
            BigDecimal winRate = BigDecimal.valueOf(dto.wins())
                    .divide(BigDecimal.valueOf(dto.matches()), 5, RoundingMode.HALF_UP);
            short bucket = dto.bucket() == null ? 0 : dto.bucket().shortValue();
            BigDecimal avgBuy = dto.avgBuyTimeS() == null ? null
                    : BigDecimal.valueOf(dto.avgBuyTimeS()).setScale(3, RoundingMode.HALF_UP);
            jdbc.update("""
                    INSERT INTO item_stats_snapshot
                        (snapshot_date, patch_id, item_id, rank_bucket, matches, wins, losses, players, win_rate, avg_buy_time_s)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (snapshot_date, patch_id, item_id, rank_bucket)
                    DO UPDATE SET
                        matches        = EXCLUDED.matches,
                        wins           = EXCLUDED.wins,
                        losses         = EXCLUDED.losses,
                        players        = EXCLUDED.players,
                        win_rate       = EXCLUDED.win_rate,
                        avg_buy_time_s = EXCLUDED.avg_buy_time_s
                    """,
                    date, patchId, dto.itemId(), bucket,
                    dto.matches(), dto.wins(), dto.losses(), dto.players() == null ? 0 : dto.players(),
                    winRate, avgBuy);
            upserted++;
        }
        log.info("Item stats upserted: {} rows for {}", upserted, date);
    }
}
