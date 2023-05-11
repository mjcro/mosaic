package io.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles data read and write using configured type handler resolvers.
 * Requires established connection for each method invocation and works
 * within transaction isolation context.
 */
public class TransactionalRepository<Key extends Enum<Key> & KeySpec> extends AbstractRepository<Key> {
    /**
     * Constructs data provider.
     *
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class to work with.
     * @param tablePrefix         Database table prefix.
     */
    public TransactionalRepository(
            TypeHandlerResolver typeHandlerResolver,
            Class<Key> clazz,
            String tablePrefix
    ) {
        super(typeHandlerResolver, clazz, tablePrefix);
    }

    /**
     * Stores given data into database.
     *
     * @param connection Database connection to use.
     * @param id         Identifier of entity data belongs to.
     * @param values     Data values.
     * @throws SQLException On database error.
     */
    public void store(Connection connection, long id, Map<Key, List<Object>> values) throws SQLException {
        Objects.requireNonNull(connection, "connection");
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

        for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
            typeHandlers.get(entry.getKey()).store(
                    connection,
                    tablePrefix,
                    id,
                    new HashMap<>(entry.getValue())
            );
        }
    }

    /**
     * Fetches data for given single entity identifier.
     *
     * @param connection Database connection to use.
     * @param id         Entity identifier.
     * @return Found data. Will return empty map if no data present.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(Connection connection, long id) throws SQLException {
        Map<Long, Map<Key, List<Object>>> map = findById(connection, Collections.singleton(id));
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return map.get(id);
    }

    /**
     * Fetches data for given identifiers.
     *
     * @param connection  Database connection to use.
     * @param identifiers Entity identifiers to fetch data for.
     * @return Found data.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(
            Connection connection,
            Collection<Long> identifiers
    ) throws SQLException {
        return find(connection, identifiers, groupByClass());
    }


    /**
     * Fetches partial data for given single entity identifier.
     *
     * @param connection Database connection.
     * @param id         Entity identifier.
     * @param keys       Keys to read.
     * @return Found data. Will return empty map if no data present.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(Connection connection, long id, Collection<Key> keys) throws SQLException {
        Map<Long, Map<Key, List<Object>>> map = findById(connection, Collections.singleton(id), keys);
        if (map == null || map.isEmpty() || !map.containsKey(id)) {
            return Collections.emptyMap();
        }
        return map.get(id);
    }

    /**
     * Fetches partial data for given identifiers.
     *
     * @param connection  Database connection.
     * @param identifiers Entity identifiers to fetch data for.
     * @param keys        Keys to read.
     * @return Found data.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(Connection connection, Collection<Long> identifiers, Collection<Key> keys) throws SQLException {
        return find(connection, identifiers, groupByClass(keys));
    }


    /**
     * Utility method that actually reads data from database.
     *
     * @param connection     Database connection.
     * @param identifiers    Identifiers to read.
     * @param groupedByClass Type handlers grouped by class.
     * @return Found data.
     * @throws SQLException On database error.
     */
    private Map<Long, Map<Key, List<Object>>> find(
            Connection connection,
            Collection<Long> identifiers,
            Map<Class<?>, List<Key>> groupedByClass
    ) throws SQLException {
        // Deduplication
        identifiers = identifiers instanceof Set<?>
                ? identifiers
                : new HashSet<>(identifiers);

        if (identifiers.isEmpty()) {
            return Collections.emptyMap();
        }

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        HashMap<Long, Map<Key, List<Object>>> combined = new HashMap<>();
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

        return combined;
    }

    /**
     * Deletes (partially) data from database.
     *
     * @param connection Database connection to use.
     * @param id         Entity identifier.
     * @param keys       Keys to delete.
     * @throws SQLException On database error.
     */
    public void delete(Connection connection, long id, Collection<Key> keys) throws SQLException {
        Objects.requireNonNull(connection, "connection");

        // Deduplication
        keys = keys instanceof Set<?>
                ? keys
                : new HashSet<>(keys);

        if (keys.isEmpty()) {
            return;
        }

        // Grouping by class
        Map<Class<?>, List<Key>> groupedByClass = groupByClass(keys);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
            entry.getValue().delete(connection, tablePrefix, id, groupedByClass.get(entry.getKey()));
        }
    }

    /**
     * Deletes all data for given entity identifier.
     *
     * @param connection Database connection to use.
     * @param id         Entity identifier.
     * @throws SQLException On database error.
     */
    public void delete(Connection connection, long id) throws SQLException {
        delete(connection, id, Arrays.stream(clazz.getEnumConstants()).collect(Collectors.toList()));
    }
}
