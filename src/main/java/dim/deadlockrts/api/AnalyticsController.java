package dim.deadlockrts.api;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/heroes/{heroId}/trend")
    @Cacheable("heroTrend")
    public List<AnalyticsQueryService.HeroTrendPoint> heroTrend(@PathVariable int heroId) {
        return queryService.heroTrend(heroId);
    }

    @GetMapping("/items/{itemId}/trend")
    @Cacheable("itemTrend")
    public List<AnalyticsQueryService.ItemTrendPoint> itemTrend(@PathVariable int itemId) {
        return queryService.itemTrend(itemId);
    }

    @GetMapping("/patches/{patchId}/diff")
    @Cacheable("patchDiff")
    public List<AnalyticsQueryService.PatchDiffPoint> patchDiff(@PathVariable int patchId) {
        return queryService.patchDiff(patchId);
    }

    @GetMapping("/movers")
    @Cacheable("movers")
    public List<AnalyticsQueryService.MoverPoint> movers(
            @RequestParam(defaultValue = "20") int limit) {
        return queryService.movers(Math.min(limit, 100));
    }
}
