package dim.deadlockrts.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionJob {

    private static final Logger log = LoggerFactory.getLogger(IngestionJob.class);

    private final SnapshotIngestionService ingestionService;
    private final DimensionRefreshService dimensionRefreshService;

    public IngestionJob(SnapshotIngestionService ingestionService,
                        DimensionRefreshService dimensionRefreshService) {
        this.ingestionService = ingestionService;
        this.dimensionRefreshService = dimensionRefreshService;
    }

    @Scheduled(cron = "${ingestion.cron:0 0 4 * * *}", zone = "UTC")
    public void run() {
        log.info("Ingestion job triggered");
        dimensionRefreshService.refreshHeroes();
        dimensionRefreshService.refreshItems();
        ingestionService.ingest();
        log.info("Ingestion job finished");
    }
}
