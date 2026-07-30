package dim.deadlockrts.ingestion;

import com.github.tomakehurst.wiremock.WireMockServer;
import dim.deadlockrts.domain.repository.HeroRepository;
import dim.deadlockrts.domain.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EnableWireMock
@org.springframework.test.context.ActiveProfiles("test")
class DimensionRefreshServiceIT {

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

    @Autowired
    DimensionRefreshService refreshService;

    @Autowired
    HeroRepository heroRepository;

    @Autowired
    ItemRepository itemRepository;

    @BeforeEach
    void resetDb() {
        heroRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void refreshHeroes_persistsHeroesFromApi() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/heroes"))
                .willReturn(okJson("""
                        [
                          {"hero_id": 1, "class_name": "hero_infernus", "display_name": "Infernus"},
                          {"hero_id": 2, "class_name": "hero_bebop",    "display_name": "Bebop"}
                        ]
                        """)));

        refreshService.refreshHeroes();

        assertThat(heroRepository.count()).isEqualTo(2);
        assertThat(heroRepository.findById(1))
                .isPresent()
                .hasValueSatisfying(h -> {
                    assertThat(h.getDisplayName()).isEqualTo("Infernus");
                    assertThat(h.getClassName()).isEqualTo("hero_infernus");
                });
    }

    @Test
    void refreshHeroes_isIdempotent() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/heroes"))
                .willReturn(okJson("""
                        [{"hero_id": 1, "class_name": "hero_infernus", "display_name": "Infernus"}]
                        """)));

        refreshService.refreshHeroes();
        refreshService.refreshHeroes();

        assertThat(heroRepository.count()).isEqualTo(1);
    }

    @Test
    void refreshItems_persistsItemsFromApi() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/items"))
                .willReturn(okJson("""
                        [
                          {"item_id": 10, "class_name": "upgrade_extra_health",   "display_name": "Extra Health"},
                          {"item_id": 11, "class_name": "upgrade_extra_stamina",  "display_name": "Extra Stamina"}
                        ]
                        """)));

        refreshService.refreshItems();

        assertThat(itemRepository.count()).isEqualTo(2);
        assertThat(itemRepository.findById(10))
                .isPresent()
                .hasValueSatisfying(i -> assertThat(i.getDisplayName()).isEqualTo("Extra Health"));
    }

    @Test
    void refreshItems_isIdempotent() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/items"))
                .willReturn(okJson("""
                        [{"item_id": 10, "class_name": "upgrade_extra_health", "display_name": "Extra Health"}]
                        """)));

        refreshService.refreshItems();
        refreshService.refreshItems();

        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    void refreshHeroes_skipsEntriesWithNullFields() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/assets/heroes"))
                .willReturn(okJson("""
                        [
                          {"hero_id": 1,    "class_name": "hero_infernus", "display_name": "Infernus"},
                          {"hero_id": null, "class_name": "hero_unknown",  "display_name": "Unknown"}
                        ]
                        """)));

        refreshService.refreshHeroes();

        assertThat(heroRepository.count()).isEqualTo(1);
    }
}
