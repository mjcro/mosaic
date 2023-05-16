package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.sql.ConnectionConsumer;
import io.github.mjcro.interfaces.sql.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles data read and write using configured type handler resolvers.
 */
public class ParallelRepository<Key extends Enum<Key> & KeySpec> extends AbstractConnectionProviderRepository<Key> {
    private final ExecutorService executorService;

    /**
     * Constructs new repository instance.
     *
     * @param executorService     Executor service to use to parallel queries.
     * @param connectionProvider  Database connection provider.
     * @param typeHandlerResolver Type handler resolver.
     * @param clazz               Key class this repository instance should work with.
     * @param tablePrefix         Database table prefix.
     */
    public ParallelRepository(
            ExecutorService executorService,
            ConnectionProvider connectionProvider,
            TypeHandlerResolver typeHandlerResolver,
            Class<Key> clazz,
            String tablePrefix
    ) {
        super(connectionProvider, typeHandlerResolver, clazz, tablePrefix);
        this.executorService = Objects.requireNonNull(executorService, "executorService");
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

        ArrayList<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<Class<?>, Map<Key, List<Object>>> entry : groupedByClass.entrySet()) {
            Future<?> future = executorService.submit(() -> {
                try {
                    connectionProvider.invokeWithConnection(connection -> {
                        typeHandlers.get(entry.getKey()).store(
                                connection,
                                tablePrefix,
                                id,
                                new HashMap<>(entry.getValue())
                        );
                    });
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Waiting for futures to complete
        waitAll(futures);
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
        Collection<Long> identifierSet = identifiers instanceof Set<?>
                ? identifiers
                : new HashSet<>(identifiers);

        // Preparing type handlers and verifying that they are present
        HashMap<Class<?>, TypeHandler> typeHandlers = new HashMap<>();
        for (Class<?> clazz : groupedByClass.keySet()) {
            TypeHandler handler = registeredTypes.resolve(clazz);
            typeHandlers.put(clazz, handler);
        }

        BlockingQueue<Map<Long, Map<Key, List<Object>>>> responses = new LinkedBlockingQueue<>();

        ArrayList<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
            Future<?> future = executorService.submit(() -> {
                try {
                    connectionProvider.invokeWithConnection(connection -> {
                        Map<Long, Map<Key, List<Object>>> data = entry.getValue().findByLinkId(
                                connection,
                                tablePrefix,
                                identifierSet,
                                groupedByClass.get(entry.getKey())
                        );
                        try {
                            responses.put(data);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Waiting for futures to complete
        waitAll(futures);

        // Merging responses
        HashMap<Long, Map<Key, List<Object>>> combined = new HashMap<>();
        for (Map<Long, Map<Key, List<Object>>> data : responses) {
            for (Map.Entry<Long, Map<Key, List<Object>>> datum : data.entrySet()) {
                if (!combined.containsKey(datum.getKey())) {
                    combined.put(datum.getKey(), new HashMap<>());
                }
                combined.get(datum.getKey()).putAll(datum.getValue());
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

        ArrayList<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<Class<?>, TypeHandler> entry : typeHandlers.entrySet()) {
            Future<?> future = executorService.submit(() -> {
                try {
                    connectionProvider.invokeWithConnection(connection -> {
                        entry.getValue().delete(connection, tablePrefix, id, groupedByClass.get(entry.getKey()));
                    });
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Waiting for futures to complete
        waitAll(futures);
    }

    /**
     * Waits for all futures to complete.
     *
     * @param futures Futures to wait.
     */
    private void waitAll(List<Future<?>> futures) {
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            // Freeing other futures (that may not be started yet)
            for (Future<?> future : futures) {
                if (!future.isDone() && !future.isCancelled()) {
                    future.cancel(false);
                }
            }
            throw new RuntimeException(e);
        }
    }
}
