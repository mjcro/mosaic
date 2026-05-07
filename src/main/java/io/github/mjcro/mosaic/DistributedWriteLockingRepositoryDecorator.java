package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.Decorator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decorator that wraps every mutating operation ({@code store}, {@code delete}) of an
 * underlying {@link AbstractConnectionProviderRepository} in a distributed lock scoped to
 * the entity identifier.
 * <p>
 * Read operations are forwarded directly without acquiring a lock. Empty value or key
 * collections short-circuit and never acquire a lock. Unlike the previous inheritance-based
 * variant this decorator can wrap any {@link AbstractConnectionProviderRepository} subclass,
 * including {@link Repository} and {@link ParallelRepository}.
 */
public class DistributedWriteLockingRepositoryDecorator<Key extends Enum<Key> & KeySpec>
        implements Decorator<AbstractConnectionProviderRepository<Key>> {
    private final AbstractConnectionProviderRepository<Key> decorated;
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * Constructs new write-locking repository decorator.
     *
     * @param decorated               Underlying repository to delegate to. Not nullable.
     * @param distributedLockExecutor Executor used to acquire and release distributed locks
     *                                around mutating operations. Not nullable.
     */
    public DistributedWriteLockingRepositoryDecorator(
            @NonNull AbstractConnectionProviderRepository<Key> decorated,
            @NonNull DistributedLockExecutor distributedLockExecutor
    ) {
        this.decorated = Objects.requireNonNull(decorated, "decorated");
        this.distributedLockExecutor = Objects.requireNonNull(distributedLockExecutor, "distributedLockExecutor");
    }

    @Override
    public @NonNull AbstractConnectionProviderRepository<Key> getDecorated() {
        return decorated;
    }

    /**
     * Fetches data for given single entity identifier without acquiring a distributed lock.
     *
     * @param id Entity identifier.
     * @return Found data. Will return empty map if no data present. Not nullable.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(long id) throws SQLException {
        return getDecorated().findById(id);
    }

    /**
     * Fetches data for given identifiers without acquiring a distributed lock.
     *
     * @param identifiers Entity identifiers to fetch data for. Not nullable.
     * @return Found data, grouped by identifier. Not nullable.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(Collection<Long> identifiers) throws SQLException {
        return getDecorated().findById(identifiers);
    }

    /**
     * Fetches partial data for given single entity identifier without acquiring a distributed lock.
     *
     * @param id   Entity identifier.
     * @param keys Keys to read. Not nullable.
     * @return Found data. Will return empty map if no data present. Not nullable.
     * @throws SQLException On database error.
     */
    public Map<Key, List<Object>> findById(long id, Collection<Key> keys) throws SQLException {
        return getDecorated().findById(id, keys);
    }

    /**
     * Fetches partial data for given identifiers without acquiring a distributed lock.
     *
     * @param identifiers Entity identifiers to fetch data for. Not nullable.
     * @param keys        Keys to read. Not nullable.
     * @return Found data, grouped by identifier. Not nullable.
     * @throws SQLException On database error.
     */
    public Map<Long, Map<Key, List<Object>>> findById(Collection<Long> identifiers, Collection<Key> keys) throws SQLException {
        return getDecorated().findById(identifiers, keys);
    }

    /**
     * Stores given data into database while holding a distributed lock bound to the given identifier.
     * No-op (and no lock acquisition) when {@code values} is null or empty.
     *
     * @param id     Identifier of entity data belongs to.
     * @param values Data values to persist. Nullable; null or empty maps are skipped.
     * @throws SQLException On database error or lock acquisition failure.
     */
    public void store(long id, @Nullable Map<Key, List<Object>> values) throws SQLException {
        if (values == null || values.isEmpty()) {
            return;
        }
        distributedLockExecutor.executeLocked(
                id,
                () -> getDecorated().store(id, values)
        );
    }

    /**
     * Deletes selected data for given entity identifier while holding a distributed lock
     * bound to the identifier. No-op (and no lock acquisition) when {@code keys} is null or empty.
     *
     * @param id   Entity identifier.
     * @param keys Keys to delete. Nullable; null or empty collections are skipped.
     * @throws SQLException On database error or lock acquisition failure.
     */
    public void delete(long id, @Nullable Collection<Key> keys) throws SQLException {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        distributedLockExecutor.executeLocked(
                id,
                () -> getDecorated().delete(id, keys)
        );
    }

    /**
     * Deletes all data for given entity identifier while holding a distributed lock
     * bound to the identifier.
     *
     * @param id Entity identifier.
     * @throws SQLException On database error or lock acquisition failure.
     */
    public void delete(long id) throws SQLException {
        distributedLockExecutor.executeLocked(
                id,
                () -> getDecorated().delete(id)
        );
    }
}
