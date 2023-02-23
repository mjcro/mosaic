package org.github.mjcro.mosaic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class Entity<Key> {
    private final Long id;
    private final Map<Key, List<Object>> values;

    public Entity(final Long id, final Map<Key, List<Object>> values) {
        this.id = id;
        this.values = values;
    }

    public OptionalLong getId() {
        return OptionalLong.of(id);
    }

    public Map<Key, List<Object>> getValues() {
        return values;
    }

    public Optional<Object> getSingle(Key key) {
        return Optional.ofNullable(values.get(key));
    }
}
