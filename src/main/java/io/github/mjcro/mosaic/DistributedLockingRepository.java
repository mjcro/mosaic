package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.sql.ConnectionProvider;
import org.jspecify.annotations.NonNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repository variant that wraps every mutating operation ({@code store}, {@code delete})
 * in a distributed lock scoped to the entity identifier.
 * <p>
 * Read operations inherited from {@link Repository} are not locked. Empty value or key
 * collections are skipped without acquiring a lock.
 */
public class DistributedLockingRepository<Key extends Enum<Key> & KeySpec> extends Repository<Key> {
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * Constructs new locking repository instance.
     *
     * @param distributedLockExecutor Executor used to acquire distributed locks. Not nullable.
     * @param connectionProvider      Database connection provider. Not nullable.
     * @param typeHandlerResolver     Type handler resolver. Not nullable.
     * @param clazz                   Key class this repository instance should work with. Not nullable.
     * @param tablePrefix             Database table prefix. Not nullable.
     */
    public DistributedLockingRepository(
            @NonNull DistributedLockExecutor distributedLockExecutor,
            @NonNull ConnectionProvider connectionProvider,
            @NonNull TypeHandlerResolver typeHandlerResolver,
            @NonNull Class<Key> clazz,
            @NonNull String tablePrefix
    ) {
        super(connectionProvider, typeHandlerResolver, clazz, tablePrefix);
        this.distributedLockExecutor = Objects.requireNonNull(distributedLockExecutor, "distributedLockExecutor");
    }

    @Override
    public void store(long id, Map<Key, List<Object>> values) throws SQLException {
        if (values == null || values.isEmpty()) {
            return;
        }
        distributedLockExecutor.executeLocked(
                id,
                () -> DistributedLockingRepository.super.store(id, values)
        );
    }

    @Override
    public void delete(long id, Collection<Key> keys) throws SQLException {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        distributedLockExecutor.executeLocked(
                id,
                () -> DistributedLockingRepository.super.delete(id, keys)
        );
    }

    @Override
    public void delete(long id) throws SQLException {
        distributedLockExecutor.executeLocked(
                id,
                () -> DistributedLockingRepository.super.delete(id)
        );
    }
}
