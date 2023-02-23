package org.github.mjcro.mosaic.util;

import org.github.mjcro.mosaic.KeySpec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper designed to simplify values map creation process.
 *
 * @param <Key>
 */
public class EnumMapBuilder<Key extends Enum<Key> & KeySpec> {
    private final EnumMap<Key, List<Object>> map;

    /**
     * Constructs new enumeration map builder.
     *
     * @param clazz Enumeration class.
     */
    public EnumMapBuilder(final Class<Key> clazz) {
        this.map = new EnumMap<>(clazz);
    }

    /**
     * @return Built values map.
     */
    public Map<Key, List<Object>> build() {
        return this.map;
    }

    /**
     * Puts single value to builder.
     *
     * @param key   Map key.
     * @param value Map value.
     * @return Builder itself.
     */
    public EnumMapBuilder<Key> putSingle(final Key key, final Object value) {
        this.map.put(key, Collections.singletonList(value));
        return this;
    }

    /**
     * Puts non-null single value to builder.
     * Handles nulls and optionals.
     *
     * @param key   Map key.
     * @param value Map value.
     * @return Builder itself.
     */
    public EnumMapBuilder<Key> putSingleIfPresent(final Key key, final Object value) {
        if (value instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) value;
            if (optional.isPresent()) {
                return putSingle(key, optional.get());
            }
        } else if (value != null) {
            return putSingle(key, value);
        }

        return this;
    }
}
