package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DataProvider<ID, Key extends Enum<Key> & KeySpec> {
    private final Supplier<Connection> connectionSupplier = null; // TODO
    private final Map<Class<?>, TypeHandler> registeredTypes = null; // TODO

    private Map<Class<?>, Map<Key, List<Object>>> groupByClass(Map<Key, List<Object>> values) {
        return null; // TODO
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

    public ID create(String tablePrefix, Map<Key, List<Object>> values) throws SQLException {
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
            ID id = null;

            // Saving other entities
            for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
                typeHandlers.get(entry.getKey()).create(connection,
                        null,
                        entry.getValue()
                );
            }

            return id;
        }
    }

    public List<Entity<ID, Key>> findById(Class<Key> keyClass, String tablePrefix, Set<ID> identifiers) throws SQLException {
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

        try (Connection connection = connectionSupplier.get()) {

        }
    }
}
