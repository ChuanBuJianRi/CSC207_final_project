package use_case.notes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes user-entered tags before they reach storage.
 */
public final class NoteTags {
    public static final int MAX_TAGS = 20;
    public static final int MAX_TAG_LENGTH = 32;

    private NoteTags() {
    }

    public static List<String> normalize(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : tags) {
            if (raw == null) {
                continue;
            }
            String tag = raw.trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                tag = tag.substring(0, MAX_TAG_LENGTH).trim();
            }
            if (tag.isEmpty()) {
                continue;
            }
            unique.putIfAbsent(tag.toLowerCase(Locale.ROOT), tag);
            if (unique.size() >= MAX_TAGS) {
                break;
            }
        }
        return List.copyOf(new ArrayList<>(unique.values()));
    }
}
