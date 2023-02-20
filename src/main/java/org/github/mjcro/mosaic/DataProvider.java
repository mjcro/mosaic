package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DataProvider<Key extends Enum<Key> & KeySpec> {
    private final Supplier<Connection> connectionSupplier = null; // TODO
    private final Map<Class<?>, TypeHandler> registeredTypes = null; // TODO

    private Map<Class<?>, Map<Key, List<Object>>> groupByClass(Map<Key, List<Object>> values) {
        HashMap<Class<?>, Map<Key, List<Object>>> grouped = new HashMap<>();
        for (final Map.Entry<Key, List<Object>> entry : values.entrySet()) {
            Class<?> clazz = entry.getKey().getDataClass();
            if (!grouped.containsKey(clazz)) {
                grouped.put(clazz, new HashMap<>());
            }
            grouped.get(clazz).put(entry.getKey(), entry.getValue());
        }
        return grouped;
    }

    private Map<Class<?>, List<Key>> groupByClass(Class<Key> clazz) {
        HashMap<Class<?>, List<Key>> grouped = new HashMap<>();
        for (Key key : clazz.getEnumConstants()) {
            if (!grouped.containsKey(key.getDataClass())) {
                grouped.put(key.getDataClass(), new ArrayList<>());
            }
            grouped.get(key.getDataClass()).add(key);
        }
        return grouped;
    }

    public long create(String tablePrefix, Map<Key, List<Object>> values) throws SQLException {
        // Grouping by class
        Map<Class<?>, Map<Key, List<Object>>> groupedByClass = groupByClass(values);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.get(clazz);
            if (handler == null) {
                throw new SQLException("Foo"); // Create new exception for this
            }
            typeHandlers.put(clazz, handler);
        }

        try (Connection connection = connectionSupplier.get()) {
            // Somehow creating ID
            long id = 0L; // TODO

            // Saving other entities
            for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
                typeHandlers.get(entry.getKey()).create(connection,
                        tablePrefix,
                        id,
                        new HashMap<>(entry.getValue())
                );
            }

            return id;
        }
    }

    public void update(String tablePrefix, long id, Map<Key, List<Object>> values) throws SQLException {
        // Grouping by class
        Map<Class<?>, Map<Key, List<Object>>> groupedByClass = groupByClass(values);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.get(clazz);
            if (handler == null) {
                throw new SQLException("Foo"); // Create new exception for this
            }
            typeHandlers.put(clazz, handler);
        }

        try (Connection connection = connectionSupplier.get()) {
            for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
                typeHandlers.get(entry.getKey()).update(
                        connection,
                        tablePrefix,
                        id,
                        new HashMap<>(entry.getValue())
                );
            }
        }
    }

    public List<Entity<Key>> findById(Class<Key> keyClass, String tablePrefix, Set<Long> identifiers) throws SQLException {
        // Grouping by class
        Map<Class<?>, List<Key>> groupedByClass = groupByClass(keyClass);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.get(clazz);
            if (handler == null) {
                throw new SQLException("Foo"); // Create new exception for this
            }
            typeHandlers.put(clazz, handler);
        }

        HashMap<Long, Map<Key, List<Object>>> combined = new HashMap<>();
        try (Connection connection = connectionSupplier.get()) {
            for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
                Map<Long, Map<Key, List<Object>>> data = entry.getValue().findById(
                        connection,
                        tablePrefix,
                        identifiers,
                        groupedByClass.get(entry.getKey())
                );
                for (Map.Entry<Long, Map<Key, List<Object>>> datum : data.entrySet()) {
                    if (!combined.containsKey(datum.getKey())) {
                        combined.put(datum.getKey(), new HashMap<>());
                    }
                    combined.get(datum.getKey()).putAll(datum.getValue());
                }
            }
        }

        return combined.entrySet().stream()
                .map($entry -> new Entity<>($entry.getKey(), $entry.getValue()))
                .collect(Collectors.toList());
    }
}
