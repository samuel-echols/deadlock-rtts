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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

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

    int patch1Id;
    int patch2Id;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE item_stats_snapshot");
        jdbc.execute("TRUNCATE TABLE hero_stats_snapshot CASCADE");
        jdbc.execute("DELETE FROM patches");
        itemRepository.deleteAll();
        heroRepository.deleteAll();

        heroRepository.save(new Hero(1, "hero_infernus", "Infernus"));
        itemRepository.save(new Item(10, "upgrade_extra_health", "Extra Health"));

        Patch p1 = patchRepository.save(new Patch(6630));
        Patch p2 = patchRepository.save(new Patch(6637));
        patch1Id = p1.getPatchId();
        patch2Id = p2.getPatchId();

        LocalDate day1 = LocalDate.now().minusDays(2);
        LocalDate day2 = LocalDate.now().minusDays(1);

        jdbc.update("""
                INSERT INTO hero_stats_snapshot
                    (snapshot_date,patch_id,hero_id,rank_bucket,matches,wins,losses,win_rate)
                VALUES (?,?,1,0,1000,480,520,0.48000)
                """, day1, patch1Id);
        jdbc.update("""
                INSERT INTO hero_stats_snapshot
                    (snapshot_date,patch_id,hero_id,rank_bucket,matches,wins,losses,win_rate)
                VALUES (?,?,1,0,1200,660,540,0.55000)
                """, day2, patch2Id);

        jdbc.update("""
                INSERT INTO item_stats_snapshot
                    (snapshot_date,patch_id,item_id,rank_bucket,matches,wins,losses,players,win_rate,avg_buy_time_s)
                VALUES (?,?,10,0,500,260,240,400,0.52000,825.000)
                """, day2, patch2Id);

        jdbc.execute("REFRESH MATERIALIZED VIEW hero_movers");
    }

    @Test
    void heroTrend_returnsSeries() throws Exception {
        mockMvc.perform(get("/api/heroes/1/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].winRate").value(0.48))
                .andExpect(jsonPath("$[1].winRate").value(0.55));
    }

    @Test
    void itemTrend_returnsSeries() throws Exception {
        mockMvc.perform(get("/api/items/10/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].winRate").value(0.52));
    }

    @Test
    void patchDiff_returnsDeltasForPatch() throws Exception {
        mockMvc.perform(get("/api/patches/" + patch2Id + "/diff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Infernus"))
                .andExpect(jsonPath("$[0].winRate").value(0.55))
                .andExpect(jsonPath("$[0].prevWinRate").value(0.48));
    }

    @Test
    void movers_returnsBiggestMovers() throws Exception {
        mockMvc.perform(get("/api/movers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Infernus"))
                .andExpect(jsonPath("$[0].winRateDelta").value(0.07));
    }

    @Test
    void movers_respectsLimitParam() throws Exception {
        mockMvc.perform(get("/api/movers?limit=5"))
                .andExpect(status().isOk());
    }
}
