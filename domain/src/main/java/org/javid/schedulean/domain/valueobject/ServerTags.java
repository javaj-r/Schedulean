package org.javid.schedulean.domain.valueobject;

import java.util.Set;

public record ServerTags(Set<String> tags) {
    public ServerTags {
        // Defensive copy using Set.copyOf (throws NullPointerException if any element is null)
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static ServerTags empty() {
        return new ServerTags(Set.of());
    }

    public static ServerTags of(String... tags) {
        return new ServerTags(Set.of(tags));
    }
}
