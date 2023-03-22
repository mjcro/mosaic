package io.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class containing helper methods.
 */
abstract class AbstractRepository<Key extends Enum<Key> & KeySpec> {
    protected final Class<Key> clazz;
    protected final String tablePrefix;
    protected final ConnectionProvider connectionProvider;
    protected final TypeHandlerResolver registeredTypes;

    /**
     * Constructs data provider.
     *
     * @param connectionProvider  Connection provider.
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class to work with.
     * @param tablePrefix         Database table prefix.
     */
    protected AbstractRepository(
            final ConnectionProvider connectionProvider,
            final TypeHandlerResolver typeHandlerResolver,
            final Class<Key> clazz,
            final String tablePrefix
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionSupplier");
        this.registeredTypes = Objects.requireNonNull(typeHandlerResolver, "registeredTypes");
        this.clazz = Objects.requireNonNull(clazz, "clazz");
        this.tablePrefix = Objects.requireNonNull(tablePrefix, "tablePrefix");
    }

    /**
     * Tests connection.
     *
     * @throws SQLException On connection error.
     */
    @SuppressWarnings("EmptyTryBlock")
    public void test() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
        }
    }

    /**
     * Groups keys by backed class.
     *
     * @param keys Keys collection.
     * @return Map of grouped per class keys.
     */
    protected Map<Class<?>, List<Key>> groupByClass(Iterable<Key> keys) {
        HashMap<Class<?>, List<Key>> grouped = new HashMap<>();
        for (Key key : keys) {
            if (!grouped.containsKey(key.getDataClass())) {
                grouped.put(key.getDataClass(), new ArrayList<>());
            }
            grouped.get(key.getDataClass()).add(key);
        }
        return grouped;
    }

    /**
     * Groups keys by backed class.
     *
     * @return Map of grouped per class keys.
     */
    protected Map<Class<?>, List<Key>> groupByClass() {
        return groupByClass(Arrays.asList(clazz.getEnumConstants()));
    }

    /**
     * Groups key value pairs by backed class.
     *
     * @param values Values map.
     * @return Map of grouped per class key value pairs.
     */
    protected Map<Class<?>, Map<Key, List<Object>>> groupByClass(Map<Key, List<Object>> values) {
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
}
