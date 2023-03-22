package io.github.mjcro.mosaic;

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
public class Repository<Key extends Enum<Key> & KeySpec> extends AbstractRepository<Key> {
    /**
     * Constructs new repository instance.
     *
     * @param connectionProvider  Database connection provider.
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class this repository instance should work with.
     * @param tablePrefix         Database table prefix.
     */
    public Repository(
            final ConnectionProvider connectionProvider,
            final TypeHandlerResolver typeHandlerResolver,
            final Class<Key> clazz,
            final String tablePrefix
    ) {
        super(connectionProvider, typeHandlerResolver, clazz, tablePrefix);
    }

    /**
     * Stores given data into database.
     *
     * @param id     Identifier of entity data belongs to.
     * @param values Data values.
     * @throws SQLException On database error.
     */
    public void store(long id, Map<Key, List<Object>> values) throws SQLException {
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

    /**
     * Fetches data for given single entity identifier.
     *
     * @param id Entity identifier.
     * @return Found data. Will return empty map if no data present.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(long id) throws SQLException {
        Map<Long, Map<Key, List<Object>>> map = findById(Collections.singleton(id));
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return map.get(id);
    }

    /**
     * Fetches data for given identifiers.
     *
     * @param identifiers Entity identifiers to fetch data for.
     * @return Found data.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(Collection<Long> identifiers) throws SQLException {
        // Deduplication
        identifiers = identifiers instanceof Set<?>
                ? identifiers
                : new HashSet<>(identifiers);

        // Grouping by class
        Map<Class<?>, List<Key>> groupedByClass = groupByClass();

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

    /**
     * Deletes (partially) data from database.
     *
     * @param id   Entity identifier.
     * @param keys Keys to delete.
     * @throws SQLException On database error.
     */
    public void delete(long id, Collection<Key> keys) throws SQLException {
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

    /**
     * Deletes all data for given entity identifier.
     *
     * @param id Entity identifier.
     * @throws SQLException On database error.
     */
    public void delete(long id) throws SQLException {
        // Grouping by class
        Map<Class<?>, List<Key>> groupedByClass = groupByClass();

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
