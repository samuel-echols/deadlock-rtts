package dim.deadlockrts.api;

import dim.deadlockrts.domain.Hero;
import dim.deadlockrts.domain.Item;
import dim.deadlockrts.domain.Patch;
import dim.deadlockrts.domain.repository.HeroRepository;
import dim.deadlockrts.domain.repository.ItemRepository;
import dim.deadlockrts.domain.repository.PatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnalyticsControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("deadlock")
            .withUsername("deadlock")
            .withPassword("deadlock");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired HeroRepository heroRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired PatchRepository patchRepository;
    @Autowired CacheManager cacheManager;

    int patch0Id;
    int patch1Id;
    int patch2Id;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        jdbc.execute("TRUNCATE TABLE item_stats_snapshot");
        jdbc.execute("TRUNCATE TABLE hero_stats_snapshot CASCADE");
        jdbc.execute("DELETE FROM patches");
        itemRepository.deleteAll();
        heroRepository.deleteAll();

        heroRepository.save(new Hero(1, "hero_infernus", "Infernus"));
        itemRepository.save(new Item(4204808176L, "upgrade_extra_health", "Extra Health"));

        patch0Id = patchRepository.save(new Patch(6620)).getPatchId();
        patch1Id = patchRepository.save(new Patch(6630)).getPatchId();
        patch2Id = patchRepository.save(new Patch(6637)).getPatchId();

        // patch0: 0.40 | patch1: 480+500 / 2000 = 0.49 | patch2: 660+440 / 2000 = 0.55
        insertHeroDay(6, patch0Id, 1000, 400);
        insertHeroDay(5, patch1Id, 1000, 480);
        insertHeroDay(4, patch1Id, 1000, 500);
        insertHeroDay(3, patch2Id, 1200, 660);
        insertHeroDay(2, patch2Id, 800, 440);

        jdbc.update("""
                INSERT INTO item_stats_snapshot
                    (snapshot_date,patch_id,item_id,rank_bucket,matches,wins,losses,players,win_rate,avg_buy_time_s)
                VALUES (?,?,4204808176,0,500,260,240,400,0.52000,825.000)
                """, LocalDate.now().minusDays(2), patch2Id);
    }

    private void insertHeroDay(int daysAgo, int patchId, int matches, int wins) {
        jdbc.update("""
                INSERT INTO hero_stats_snapshot
                    (snapshot_date,patch_id,hero_id,rank_bucket,matches,wins,losses,win_rate)
                VALUES (?,?,1,0,?,?,?,ROUND(?::NUMERIC / ?, 5))
                """, LocalDate.now().minusDays(daysAgo), patchId, matches, wins, matches - wins, wins, matches);
    }

    @Test
    void heroTrend_returnsDailySeries() throws Exception {
        mockMvc.perform(get("/api/heroes/1/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].winRate").value(0.40))
                .andExpect(jsonPath("$[4].winRate").value(0.55));
    }

    @Test
    void itemTrend_returnsSeries() throws Exception {
        mockMvc.perform(get("/api/items/4204808176/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].winRate").value(0.52));
    }

    @Test
    void patchDiff_poolsDailySnapshotsPerPatch() throws Exception {
        mockMvc.perform(get("/api/patches/" + patch2Id + "/diff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Infernus"))
                .andExpect(jsonPath("$[0].winRate").value(0.55))
                .andExpect(jsonPath("$[0].prevWinRate").value(0.49))
                .andExpect(jsonPath("$[0].delta").value(0.06));
    }

    @Test
    void patchDiff_firstPatchHasNoPrevious() throws Exception {
        mockMvc.perform(get("/api/patches/" + patch0Id + "/diff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prevWinRate").value(nullValue()));
    }

    @Test
    void movers_onlyReturnsLatestPatch() throws Exception {
        // patch1 vs patch0 moved more (0.09) but is not the latest patch
        mockMvc.perform(get("/api/movers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Infernus"))
                .andExpect(jsonPath("$[0].patchId").value(patch2Id))
                .andExpect(jsonPath("$[0].winRateDelta").value(0.06));
    }

    @Test
    void movers_respectsLimitParam() throws Exception {
        mockMvc.perform(get("/api/movers?limit=5"))
                .andExpect(status().isOk());
    }
}
