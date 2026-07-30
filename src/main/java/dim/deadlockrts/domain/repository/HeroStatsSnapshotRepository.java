package dim.deadlockrts.domain.repository;

import dim.deadlockrts.domain.HeroStatsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroStatsSnapshotRepository extends JpaRepository<HeroStatsSnapshot, Long> {}
