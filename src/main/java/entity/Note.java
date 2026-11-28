package entity;

import java.time.Instant;
import java.util.List;

/**
 * One Markdown note with an id, title, tags and timestamps.
 */
public final class Note {
    private final String id;
    private final String title;
    private final String markdown;
    private final List<String> tags;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Note(String id, String title, String markdown, List<String> tags,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.markdown = markdown == null ? "" : markdown;
        this.tags = List.copyOf(tags == null ? List.of() : tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdown() {
        return markdown;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
