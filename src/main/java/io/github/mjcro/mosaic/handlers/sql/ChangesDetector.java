package io.github.mjcro.mosaic.handlers.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class aiming to help calculate difference between
 * data to store and data already presents in database.
 *
 * @param <ID>  ID type, Long in most cases.
 * @param <Key> Key type.
 */
public class ChangesDetector<ID, Key> {
    private final Map<Key, List<Object>> valuesToStore;
    private final Collection<StoredValue<ID, Key>> storedValues;
    private final Map<Key, List<StoredValue<ID, Key>>> storedValuesMap;

    /**
     * Constructs new changes detector.
     *
     * @param valuesToStore      Values to store into database.
     * @param valuesFromDatabase Value already presents in database.
     */
    public ChangesDetector(
            Map<Key, List<Object>> valuesToStore,
            Collection<StoredValue<ID, Key>> valuesFromDatabase
    ) {
        this.valuesToStore = Objects.requireNonNull(valuesToStore, "valuesToStore");
        this.storedValues = Objects.requireNonNull(valuesFromDatabase, "valuesFromDatabase");
        this.storedValuesMap = valuesFromDatabase
                .stream()
                .collect(Collectors.groupingBy(StoredValue::getKey));
    }

    /**
     * Calculates values that have no counterparts already stored
     * in database.
     *
     * @return Values to insert.
     */
    public Map<Key, List<Object>> calculateValuesToInsert() {
        if (valuesToStore.isEmpty() || storedValuesMap.isEmpty()) {
            return valuesToStore;
        }

        LinkedHashMap<Key, List<Object>> response = new LinkedHashMap<>();
        for (Key key : valuesToStore.keySet()) {
            // Taking unique values from stored values
            HashSet<Object> unique = storedValuesMap.getOrDefault(key, Collections.emptyList())
                    .stream()
                    .map(StoredValue::getValue)
                    .collect(Collectors.toCollection(HashSet::new));

            // Checking which values are not present
            for (Object value : valuesToStore.get(key)) {
                if (unique.contains(value)) {
                    continue;
                }

                if (!response.containsKey(key)) {
                    response.put(key, new ArrayList<>());
                }
                response.get(key).add(value);
            }
        }
        return response;
    }

    /**
     * Calculates identifiers of entities to delete.
     *
     * @return Set of identifiers.
     */
    public Set<ID> calculateIdToDelete() {
        if (storedValuesMap.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<ID> response = new LinkedHashSet<>();
        for (Key key : valuesToStore.keySet()) {
            // Taking unique from values to store
            HashSet<Object> unique = new HashSet<>(valuesToStore.get(key));

            for (StoredValue<ID, Key> value : storedValuesMap.getOrDefault(key, Collections.emptyList())) {
                if (!unique.contains(value.getValue())) {
                    response.add(value.getId());
                }
            }
        }
        return response;
    }

    /**
     * Calculates identifiers of entities that will not be changed
     * because data in it equals to storing ones.
     *
     * @return Set of identifiers.
     */
    public Set<ID> calculateIdIntact() {
        if (storedValuesMap.isEmpty()) {
            return Collections.emptySet();
        }

        LinkedHashSet<ID> response = new LinkedHashSet<>();
        for (Key key : valuesToStore.keySet()) {
            // Taking unique from values to store
            HashSet<Object> unique = new HashSet<>(valuesToStore.get(key));

            for (StoredValue<ID, Key> value : storedValuesMap.getOrDefault(key, Collections.emptyList())) {
                if (unique.contains(value.getValue())) {
                    response.add(value.getId());
                }
            }
        }
        return response;
    }

    /**
     * Represents value stored in database.
     */
    public static class StoredValue<ID, Key> {
        private final ID id;
        private final Key key;
        private final Object value;

        public StoredValue(ID id, Key key, Object value) {
            this.id = Objects.requireNonNull(id, "id");
            this.key = Objects.requireNonNull(key, "key");
            this.value = Objects.requireNonNull(value, "value");
        }

        public ID getId() {
            return id;
        }

        public Key getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
