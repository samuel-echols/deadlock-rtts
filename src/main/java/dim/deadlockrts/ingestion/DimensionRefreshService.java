package dim.deadlockrts.ingestion;

import dim.deadlockrts.client.DeadlockAssetsClient;
import dim.deadlockrts.client.HeroAssetDto;
import dim.deadlockrts.client.ItemAssetDto;
import dim.deadlockrts.domain.Hero;
import dim.deadlockrts.domain.Item;
import dim.deadlockrts.domain.repository.HeroRepository;
import dim.deadlockrts.domain.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DimensionRefreshService {

    private static final Logger log = LoggerFactory.getLogger(DimensionRefreshService.class);

    private final DeadlockAssetsClient assetsClient;
    private final HeroRepository heroRepository;
    private final ItemRepository itemRepository;

    public DimensionRefreshService(DeadlockAssetsClient assetsClient,
                                   HeroRepository heroRepository,
                                   ItemRepository itemRepository) {
        this.assetsClient = assetsClient;
        this.heroRepository = heroRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void refreshHeroes() {
        List<HeroAssetDto> dtos = assetsClient.fetchHeroes();
        int upserted = 0;
        for (HeroAssetDto dto : dtos) {
            if (dto.heroId() == null || dto.className() == null || dto.displayName() == null) {
                log.warn("Skipping hero with null required field: {}", dto);
                continue;
            }
            heroRepository.findById(dto.heroId()).ifPresentOrElse(
                    hero -> hero.update(dto.className(), dto.displayName()),
                    () -> heroRepository.save(new Hero(dto.heroId(), dto.className(), dto.displayName()))
            );
            upserted++;
        }
        log.info("Hero dimension refresh complete: {} upserted", upserted);
    }

    @Transactional
    public void refreshItems() {
        List<ItemAssetDto> dtos = assetsClient.fetchItems();
        int upserted = 0;
        for (ItemAssetDto dto : dtos) {
            if (dto.itemId() == null || dto.className() == null || dto.displayName() == null) {
                log.warn("Skipping item with null required field: {}", dto);
                continue;
            }
            itemRepository.findById(dto.itemId()).ifPresentOrElse(
                    item -> item.update(dto.className(), dto.displayName()),
                    () -> itemRepository.save(new Item(dto.itemId(), dto.className(), dto.displayName()))
            );
            upserted++;
        }
        log.info("Item dimension refresh complete: {} upserted", upserted);
    }
}
