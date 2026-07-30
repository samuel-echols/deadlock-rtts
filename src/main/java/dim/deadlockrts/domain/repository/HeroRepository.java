package dim.deadlockrts.domain.repository;

import dim.deadlockrts.domain.Hero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroRepository extends JpaRepository<Hero, Integer> {}
