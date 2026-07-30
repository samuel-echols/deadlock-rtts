package dim.deadlockrts.domain.repository;

import dim.deadlockrts.domain.Patch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatchRepository extends JpaRepository<Patch, Integer> {
    Optional<Patch> findByBuildNumber(Integer buildNumber);
}
