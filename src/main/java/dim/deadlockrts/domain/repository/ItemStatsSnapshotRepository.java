package dim.deadlockrts.domain.repository;

import dim.deadlockrts.domain.ItemStatsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemStatsSnapshotRepository extends JpaRepository<ItemStatsSnapshot, Long> {}
