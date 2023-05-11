package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.sql.ConnectionProvider;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractConnectionProviderRepository<Key extends Enum<Key> & KeySpec> extends AbstractRepository<Key> {
    protected final ConnectionProvider connectionProvider;

    /**
     * Constructs new repository instance.
     *
     * @param connectionProvider  Database connection provider.
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class this repository instance should work with.
     * @param tablePrefix         Database table prefix.
     */
    public AbstractConnectionProviderRepository(
            ConnectionProvider connectionProvider,
            TypeHandlerResolver typeHandlerResolver,
            Class<Key> clazz,
            String tablePrefix
    ) {
        super(typeHandlerResolver, clazz, tablePrefix);
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    /**
     * Utility method that actually reads data from database.
     *
     * @param identifiers    Identifiers to read.
     * @param groupedByClass Type handlers grouped by class.
     * @return Found data.
     * @throws SQLException On database error.
     */
    protected abstract Map<Long, Map<Key, List<Object>>> find(
            Collection<Long> identifiers,
            Map<Class<?>, List<Key>> groupedByClass
    ) throws SQLException;

    /**
     * Fetches data for given single entity identifier.
     *
     * @param id Entity identifier.
     * @return Found data. Will return empty map if no data present.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(long id) throws SQLException {
        Map<Long, Map<Key, List<Object>>> map = findById(Collections.singleton(id));
        if (map == null || map.isEmpty() || !map.containsKey(id)) {
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
        return find(identifiers, groupByClass());
    }

    /**
     * Fetches partial data for given single entity identifier.
     *
     * @param id   Entity identifier.
     * @param keys Keys to read.
     * @return Found data. Will return empty map if no data present.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(long id, Collection<Key> keys) throws SQLException {
        Map<Long, Map<Key, List<Object>>> map = findById(Collections.singleton(id), keys);
        if (map == null || map.isEmpty() || !map.containsKey(id)) {
            return Collections.emptyMap();
        }
        return map.get(id);
    }

    /**
     * Fetches partial data for given identifiers.
     *
     * @param identifiers Entity identifiers to fetch data for.
     * @param keys        Keys to read.
     * @return Found data.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(Collection<Long> identifiers, Collection<Key> keys) throws SQLException {
        return find(identifiers, groupByClass(keys));
    }

    /**
     * Stores given data into database.
     *
     * @param id     Identifier of entity data belongs to.
     * @param values Data values.
     * @throws SQLException On database error.
     */
    public abstract void store(long id, Map<Key, List<Object>> values) throws SQLException;

    /**
     * Deletes (partially) data from database.
     *
     * @param id   Entity identifier.
     * @param keys Keys to delete.
     * @throws SQLException On database error.
     */
    public abstract void delete(long id, Collection<Key> keys) throws SQLException;

    /**
     * Deletes all data for given entity identifier.
     *
     * @param id Entity identifier.
     * @throws SQLException On database error.
     */
    public void delete(long id) throws SQLException {
        delete(id, Arrays.stream(clazz.getEnumConstants()).collect(Collectors.toList()));
    }
}
