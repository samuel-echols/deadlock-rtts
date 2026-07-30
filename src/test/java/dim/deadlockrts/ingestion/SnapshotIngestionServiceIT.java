package dim.deadlockrts.ingestion;

import dim.deadlockrts.domain.Hero;
import dim.deadlockrts.domain.Item;
import dim.deadlockrts.domain.repository.HeroRepository;
import dim.deadlockrts.domain.repository.HeroStatsSnapshotRepository;
import dim.deadlockrts.domain.repository.ItemRepository;
import dim.deadlockrts.domain.repository.ItemStatsSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableWireMock
@ActiveProfiles("test")
class SnapshotIngestionServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("deadlock")
            .withUsername("deadlock")
            .withPassword("deadlock");

    @InjectWireMock
    WireMockServer wireMock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired SnapshotIngestionService ingestionService;
    @Autowired HeroRepository heroRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired HeroStatsSnapshotRepository heroSnapshotRepo;
    @Autowired ItemStatsSnapshotRepository itemSnapshotRepo;

    @BeforeEach
    void setUp() {
        itemSnapshotRepo.deleteAll();
        heroSnapshotRepo.deleteAll();
        itemRepository.deleteAll();
        heroRepository.deleteAll();

        heroRepository.save(new Hero(1, "hero_infernus", "Infernus"));
        itemRepository.save(new Item(7409189, "upgrade_extra_health", "Extra Health"));

        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/client-versions"))
                .willReturn(okJson("[6634, 6635, 6636, 6637]")));

        wireMock.stubFor(get(urlPathEqualTo("/v1/analytics/hero-stats"))
                .willReturn(okJson("""
                        [{"hero_id":1,"bucket":0,"wins":500,"losses":400,"matches":900}]
                        """)));

        wireMock.stubFor(get(urlPathEqualTo("/v1/analytics/item-stats"))
                .willReturn(okJson("""
                        [{"item_id":7409189,"bucket":0,"wins":300,"losses":200,"matches":500,"players":400,"avg_buy_time_s":825.3}]
                        """)));
    }

    @Test
    void ingest_persistsHeroAndItemSnapshots() {
        ingestionService.ingest();

        assertThat(heroSnapshotRepo.count()).isEqualTo(1);
        assertThat(itemSnapshotRepo.count()).isEqualTo(1);

        var heroSnap = heroSnapshotRepo.findAll().get(0);
        assertThat(heroSnap.getHeroId()).isEqualTo(1);
        assertThat(heroSnap.getMatches()).isEqualTo(900);
        assertThat(heroSnap.getWins()).isEqualTo(500);
        assertThat(heroSnap.getLosses()).isEqualTo(400);

        var itemSnap = itemSnapshotRepo.findAll().get(0);
        assertThat(itemSnap.getItemId()).isEqualTo(7409189);
        assertThat(itemSnap.getMatches()).isEqualTo(500);
    }

    @Test
    void ingest_isIdempotent() {
        ingestionService.ingest();
        ingestionService.ingest();

        assertThat(heroSnapshotRepo.count()).isEqualTo(1);
        assertThat(itemSnapshotRepo.count()).isEqualTo(1);
    }

    @Test
    void ingest_registersNewPatchAutomatically() {
        ingestionService.ingest();

        assertThat(heroSnapshotRepo.findAll().get(0).getPatchId()).isPositive();
    }
}
