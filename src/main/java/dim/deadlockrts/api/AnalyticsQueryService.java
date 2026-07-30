package dim.deadlockrts.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsQueryService {

    private final JdbcTemplate jdbc;

    public AnalyticsQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HeroTrendPoint> heroTrend(int heroId) {
        return jdbc.query("""
                SELECT s.snapshot_date, s.patch_id, p.build_number, s.matches, s.wins, s.losses, s.win_rate
                FROM hero_stats_snapshot s
                JOIN patches p ON p.patch_id = s.patch_id
                WHERE s.hero_id = ? AND s.rank_bucket = 0
                ORDER BY s.snapshot_date
                """,
                (rs, i) -> new HeroTrendPoint(
                        rs.getObject("snapshot_date", LocalDate.class),
                        rs.getInt("patch_id"),
                        rs.getInt("build_number"),
                        rs.getInt("matches"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getBigDecimal("win_rate")
                ),
                heroId);
    }

    public List<ItemTrendPoint> itemTrend(int itemId) {
        return jdbc.query("""
                SELECT s.snapshot_date, s.patch_id, p.build_number, s.matches, s.wins, s.losses,
                       s.win_rate, s.avg_buy_time_s
                FROM item_stats_snapshot s
                JOIN patches p ON p.patch_id = s.patch_id
                WHERE s.item_id = ? AND s.rank_bucket = 0
                ORDER BY s.snapshot_date
                """,
                (rs, i) -> new ItemTrendPoint(
                        rs.getObject("snapshot_date", LocalDate.class),
                        rs.getInt("patch_id"),
                        rs.getInt("build_number"),
                        rs.getInt("matches"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getBigDecimal("win_rate"),
                        rs.getBigDecimal("avg_buy_time_s")
                ),
                itemId);
    }

    public List<PatchDiffPoint> patchDiff(int patchId) {
        return jdbc.query("""
                WITH prev AS (
                    SELECT hero_id, win_rate AS prev_win_rate, patch_id AS prev_patch_id
                    FROM hero_stats_snapshot
                    WHERE patch_id = (SELECT MAX(patch_id) FROM patches WHERE patch_id < ?)
                      AND rank_bucket = 0
                )
                SELECT s.hero_id, h.display_name, s.win_rate,
                       prev.prev_win_rate,
                       (s.win_rate - prev.prev_win_rate) AS delta
                FROM hero_stats_snapshot s
                JOIN heroes h ON h.hero_id = s.hero_id
                LEFT JOIN prev ON prev.hero_id = s.hero_id
                WHERE s.patch_id = ? AND s.rank_bucket = 0
                ORDER BY delta DESC NULLS LAST
                """,
                (rs, i) -> new PatchDiffPoint(
                        rs.getInt("hero_id"),
                        rs.getString("display_name"),
                        rs.getBigDecimal("win_rate"),
                        rs.getBigDecimal("prev_win_rate"),
                        rs.getBigDecimal("delta")
                ),
                patchId, patchId);
    }

    public List<MoverPoint> movers(int limit) {
        return jdbc.query("""
                SELECT hero_id, display_name, patch_id, win_rate, prev_win_rate, win_rate_delta
                FROM hero_movers
                ORDER BY ABS(win_rate_delta) DESC
                LIMIT ?
                """,
                (rs, i) -> new MoverPoint(
                        rs.getInt("hero_id"),
                        rs.getString("display_name"),
                        rs.getInt("patch_id"),
                        rs.getBigDecimal("win_rate"),
                        rs.getBigDecimal("prev_win_rate"),
                        rs.getBigDecimal("win_rate_delta")
                ),
                limit);
    }

    // --- Response types ---

    public record HeroTrendPoint(LocalDate date, int patchId, int buildNumber,
                                 int matches, int wins, int losses, BigDecimal winRate) {}

    public record ItemTrendPoint(LocalDate date, int patchId, int buildNumber,
                                 int matches, int wins, int losses,
                                 BigDecimal winRate, BigDecimal avgBuyTimeS) {}

    public record PatchDiffPoint(int heroId, String displayName,
                                 BigDecimal winRate, BigDecimal prevWinRate, BigDecimal delta) {}

    public record MoverPoint(int heroId, String displayName, int patchId,
                             BigDecimal winRate, BigDecimal prevWinRate, BigDecimal winRateDelta) {}
}
