package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DataProvider<ID, Key extends KeySpec> {
    private final Supplier<Connection> connectionSupplier = null; // TODO
    private final Map<Class<?>, TypeHandler> registeredTypes = null; // TODO

    private Map<Class<?>, Map<Key, List<Object>>> groupByClass(Map<Key, List<Object>> values) {
        return null; // TODO
    }

    public ID create(String tablePrefix, Map<Key, List<Object>> values) throws SQLException {
        // Grouping by class
        final Map<Class<?>, Map<Key, List<Object>>> groupedByClass = groupByClass(values);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (final Class<?> clazz : groupedByClass.keySet()) {
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
            for (final Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
                typeHandlers.get(entry.getKey())
                        .create(connection, null, (Map<KeySpec, List<Object>>) entry.getValue());
            }

            return id;
        }
    }

    public List<Entity<ID, Key>> findById(String tablePrefix, Set<ID> identifiers) throws SQLException {
        try (Connection connection = connectionSupplier.get()) {

        }
    }
}
