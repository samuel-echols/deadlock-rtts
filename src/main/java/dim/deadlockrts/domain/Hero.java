package dim.deadlockrts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "heroes")
public class Hero {

    @Id
    @Column(name = "hero_id")
    private Integer heroId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    protected Hero() {}

    public Hero(Integer heroId, String className, String displayName) {
        this.heroId = heroId;
        this.className = className;
        this.displayName = displayName;
    }

    public Integer getHeroId() { return heroId; }
    public String getClassName() { return className; }
    public String getDisplayName() { return displayName; }

    public void update(String className, String displayName) {
        this.className = className;
        this.displayName = displayName;
    }
}
