package org.github.mjcro.mosaic;

import java.util.List;
import java.util.Map;

public class Entity<Key> {
    private final long id;
    private final Map<Key, List<Object>> values;

    public Entity(final long id, final Map<Key, List<Object>> values) {
        this.id = id;
        this.values = values;
    }

    public long getId() {
        return id;
    }

    public Map<Key, List<Object>> getValues() {
        return values;
    }
}
