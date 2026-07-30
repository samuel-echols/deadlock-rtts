package dim.deadlockrts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    protected Item() {}

    public Item(Integer itemId, String className, String displayName) {
        this.itemId = itemId;
        this.className = className;
        this.displayName = displayName;
    }

    public Integer getItemId() { return itemId; }
    public String getClassName() { return className; }
    public String getDisplayName() { return displayName; }

    public void update(String className, String displayName) {
        this.className = className;
        this.displayName = displayName;
    }
}
