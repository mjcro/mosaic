package io.github.mjcro.mosaic.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class to work with repository responses.
 * This class can be extended with entity interface implementations.
 */
public class ResponseContainer<Key> {
    /**
     * Data obtained from repository.
     */
    private final Map<Key, List<Object>> data;

    /**
     * Constructs new response container.
     *
     * @param data Data.
     */
    public ResponseContainer(Map<Key, List<Object>> data) {
        this.data = Collections.unmodifiableMap(Objects.requireNonNull(data, "data"));
    }

    /**
     * @return True if data is empty.
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * @return Count of keys presents in response.
     */
    public int size() {
        return data.size();
    }

    /**
     * @return Key set.
     */
    public Set<Key> keySet() {
        return data.keySet();
    }

    /**
     * Returns single value from response map associated with key.
     *
     * @param key   Key to fetch data.
     * @param clazz Extracted value class.
     * @return Value, optional.
     * @throws IllegalStateException If more than one element with such key presents in data map.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSingle(Key key, Class<T> clazz) {
        List<Object> datum = data.get(key);
        if (datum == null || datum.isEmpty()) {
            return Optional.empty();
        } else if (datum.size() > 1) {
            throw new IllegalStateException(
                    "Key " + key + " should have 1 element, but " + datum.size() + " found"
            );
        }
        return Optional.of((T) datum.get(0));
    }

    /**
     * Returns single value from response map associated with key.
     *
     * @param key   Key to fetch data.
     * @param clazz Extracted value class.
     * @return Value.
     * @throws IllegalStateException  If more than one element with such key presents in data map.
     * @throws NoSuchElementException If there is no value associated with key.
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public <T> T mustGetSingle(Key key, Class<T> clazz) {
        return getSingle(key, clazz).get();
    }

    /**
     * Returns list of values from response map associated with key.
     *
     * @param key   Key to fetch data.
     * @param clazz Extracted value class.
     * @return Values. If no data presents empty list will be returned.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(Key key, Class<T> clazz) {
        List<Object> datum = data.get(key);
        return datum == null
                ? Collections.emptyList()
                : Collections.unmodifiableList((List<T>) datum);
    }
}
