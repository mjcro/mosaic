package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.sql.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles data read and write using configured type handler resolvers.
 */
public class Repository<Key extends Enum<Key> & KeySpec> extends AbstractConnectionProviderRepository<Key> {
    /**
     * Constructs new repository instance.
     *
     * @param connectionProvider  Database connection provider.
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class this repository instance should work with.
     * @param tablePrefix         Database table prefix.
     */
    public Repository(
            ConnectionProvider connectionProvider,
            TypeHandlerResolver typeHandlerResolver,
            Class<Key> clazz,
            String tablePrefix
    ) {
        super(connectionProvider, typeHandlerResolver, clazz, tablePrefix);
    }

    @Override
    public void store(long id, Map<Key, List<Object>> values) throws SQLException {
        if (values == null || values.isEmpty()) {
            return;
        }

        // Grouping by class
        Map<Class<?>, Map<Key, List<Object>>> groupedByClass = groupByClass(values);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        try (Connection connection = connectionProvider.getConnection()) {
            for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
                typeHandlers.get(entry.getKey()).store(
                        connection,
                        tablePrefix,
                        id,
                        new HashMap<>(entry.getValue())
                );
            }
        }
    }

    @Override
    protected Map<Long, Map<Key, List<Object>>> find(
            Collection<Long> identifiers,
            Map<Class<?>, List<Key>> groupedByClass
    ) throws SQLException {
        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        // Deduplication
        identifiers = identifiers instanceof Set<?>
                ? identifiers
                : new HashSet<>(identifiers);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        HashMap<Long, Map<Key, List<Object>>> combined = new HashMap<>();
        try (Connection connection = connectionProvider.getConnection()) {
            for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
                Map<Long, Map<Key, List<Object>>> data = entry.getValue().findByLinkId(
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

        return combined;
    }

    @Override
    public void delete(long id, Collection<Key> keys) throws SQLException {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // Deduplication
        keys = keys instanceof Set<?>
                ? keys
                : new HashSet<>(keys);

        // Grouping by class
        Map<Class<?>, List<Key>> groupedByClass = groupByClass(keys);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        try (Connection connection = connectionProvider.getConnection()) {
            for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
                entry.getValue().delete(connection, tablePrefix, id, groupedByClass.get(entry.getKey()));
            }
        }
    }
}
