package org.github.mjcro.mosaic;

import java.util.List;
import java.util.Map;

public class Entity<Key extends Enum<Key> & KeySpec> {
    private final long id;
    private final Map<Key, List<Object>> values;

    public Entity(final long id, final Map<Key, List<Object>> values) {
        this.id = id;
        this.values = values;
    }
}
