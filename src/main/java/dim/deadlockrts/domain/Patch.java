package dim.deadlockrts.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "patches")
public class Patch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patch_id")
    private Integer patchId;

    @Column(name = "build_number", nullable = false, unique = true)
    private Integer buildNumber;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "notes_url", length = 500)
    private String notesUrl;

    @Column(name = "summary")
    private String summary;

    protected Patch() {}

    public Patch(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public Integer getPatchId() { return patchId; }
    public Integer getBuildNumber() { return buildNumber; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public String getNotesUrl() { return notesUrl; }
    public String getSummary() { return summary; }
}
