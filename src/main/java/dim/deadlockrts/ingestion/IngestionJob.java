package dim.deadlockrts.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class IngestionJob {

    private static final Logger log = LoggerFactory.getLogger(IngestionJob.class);

    private final SnapshotIngestionService ingestionService;
    private final DimensionRefreshService dimensionRefreshService;
    private final CacheManager cacheManager;

    public IngestionJob(SnapshotIngestionService ingestionService,
                        DimensionRefreshService dimensionRefreshService,
                        CacheManager cacheManager) {
        this.ingestionService = ingestionService;
        this.dimensionRefreshService = dimensionRefreshService;
        this.cacheManager = cacheManager;
    }

    @Scheduled(cron = "${ingestion.cron:0 0 4 * * *}", zone = "UTC")
    public void run() {
        log.info("Ingestion job triggered");
        dimensionRefreshService.refreshHeroes();
        dimensionRefreshService.refreshItems();
        ingestionService.ingest();
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);
        log.info("Ingestion job finished");
    }
}
