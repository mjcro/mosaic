package io.github.mjcro.mosaic.util;

import io.github.mjcro.mosaic.KeySpec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper designed to simplify values map creation process.
 *
 * @param <Key>
 */
public class EnumMapBuilder<Key extends Enum<Key> & KeySpec> {
    private final EnumMap<Key, List<Object>> map;

    /**
     * Constructs enumeration map builder for given enum class.
     *
     * @param clazz Enumeration class name.
     * @return Builder.
     */
    public static <X extends Enum<X> & KeySpec> EnumMapBuilder<X> ofClass(Class<X> clazz) {
        return new EnumMapBuilder<>(new EnumMap<>(Objects.requireNonNull(clazz, "clazz")));
    }

    private EnumMapBuilder(EnumMap<Key, List<Object>> data) {
        this.map = data;
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
    public EnumMapBuilder<Key> putSingle(Key key, Object value) {
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
    public EnumMapBuilder<Key> putSingleIfPresent(Key key, Object value) {
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
