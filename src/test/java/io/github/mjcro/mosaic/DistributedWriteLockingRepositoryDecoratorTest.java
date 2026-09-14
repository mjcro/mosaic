package io.github.mjcro.mosaic;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.mjcro.interfaces.concurrency.DistributedLockExecutor;
import io.github.mjcro.mosaic.handlers.sql.mappers.BigDecimalMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.LongMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithCreationTimeSeconds;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DistributedWriteLockingRepositoryDecoratorTest extends BaseRepositoryTest {
    private static Repository<Key> repository(final String database, final String tablePrefix) throws SQLException {
        DriverManager.getConnection(
                "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'"
        );
        return new Repository<>(
                () -> DriverManager.getConnection("jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1"),
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.DEFAULT, new StringMapper())
                        .with(Long.class, MySqlMinimalLayout.DEFAULT, new LongMapper())
                        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper())
                        .with(BigDecimal.class, MySqlMinimalLayout.DEFAULT, new BigDecimalMapper().withCommonName("Discount"))
                        .with(Amount.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new CustomAmountMapper()),
                Key.class,
                tablePrefix
        );
    }

    @Test
    public void testMutationsAcquireAndReleaseLock() throws SQLException {
        final RecordingLockExecutor executor = new RecordingLockExecutor();
        final DistributedWriteLockingRepositoryDecorator<Key> repository = new DistributedWriteLockingRepositoryDecorator<>(
                repository("lockingDecorator1", "unitTest"),
                executor
        );

        repository.store(8L, EnumMapBuilder.ofClass(Key.class).putSingle(Key.FIRST_NAME, "John").build());
        repository.delete(8L, Collections.singleton(Key.FIRST_NAME));
        repository.delete(8L);

        Assertions.assertEquals(3, executor.acquired.size(), "Each mutation must acquire the lock exactly once");
        Assertions.assertEquals(Collections.singleton(8L), new java.util.HashSet<>(executor.acquired), "Lock must be keyed by entity identifier");
        Assertions.assertEquals(executor.acquired, executor.released, "Every acquired lock must be released");
        Assertions.assertEquals(0, executor.held, "No lock may remain held");
    }

    @Test
    public void testReadsDoNotAcquireLock() throws SQLException {
        final RecordingLockExecutor executor = new RecordingLockExecutor();
        final DistributedWriteLockingRepositoryDecorator<Key> repository = new DistributedWriteLockingRepositoryDecorator<>(
                repository("lockingDecorator2", "unitTest"),
                executor
        );

        repository.findById(8L);
        repository.findById(Collections.singleton(8L));
        repository.findById(8L, Collections.singleton(Key.FIRST_NAME));
        repository.findById(Collections.singleton(8L), Collections.singleton(Key.FIRST_NAME));

        Assertions.assertTrue(executor.acquired.isEmpty(), "Reads must pass through unlocked");
    }

    @Test
    public void testEmptyMutationsDoNotAcquireLock() throws SQLException {
        final RecordingLockExecutor executor = new RecordingLockExecutor();
        final DistributedWriteLockingRepositoryDecorator<Key> repository = new DistributedWriteLockingRepositoryDecorator<>(
                repository("lockingDecorator3", "unitTest"),
                executor
        );

        repository.store(8L, null);
        repository.store(8L, Collections.emptyMap());
        repository.delete(8L, null);
        repository.delete(8L, Collections.emptyList());

        Assertions.assertTrue(executor.acquired.isEmpty(), "Empty mutations must short-circuit before locking");
    }

    @Test
    public void testDatabaseFailureSurfacesAsSQLException() throws SQLException {
        final RecordingLockExecutor executor = new RecordingLockExecutor();
        // Table prefix with no matching tables, so the underlying repository fails.
        final DistributedWriteLockingRepositoryDecorator<Key> repository = new DistributedWriteLockingRepositoryDecorator<>(
                repository("lockingDecorator4", "missing"),
                executor
        );

        Assertions.assertThrows(
                SQLException.class,
                () -> repository.store(8L, EnumMapBuilder.ofClass(Key.class).putSingle(Key.FIRST_NAME, "John").build()),
                "Database failure must reach the caller as SQLException, not wrapped in a RuntimeException"
        );
        Assertions.assertEquals(1, executor.released.size(), "Lock must be released when the mutation fails");
        Assertions.assertEquals(0, executor.held, "No lock may remain held after a failure");
    }

    @Test
    public void testAcceptsSupertypeKeyedExecutor() throws SQLException {
        final DistributedLockExecutor<Object> executor = new DistributedLockExecutor<Object>() {
            @Override
            public <R> R executeLocked(final Object lockingKey, final Supplier<R> supplier) {
                return supplier.get();
            }
        };

        // Compiles because the decorator accepts DistributedLockExecutor<? super Long>.
        final DistributedWriteLockingRepositoryDecorator<Key> repository = new DistributedWriteLockingRepositoryDecorator<>(
                repository("lockingDecorator5", "unitTest"),
                executor
        );

        repository.store(8L, EnumMapBuilder.ofClass(Key.class).putSingle(Key.FIRST_NAME, "John").build());
        Assertions.assertEquals("John", repository.findById(8L).get(Key.FIRST_NAME).get(0));
    }

    /**
     * Lock executor recording every acquisition and release.
     */
    private static final class RecordingLockExecutor implements DistributedLockExecutor<Long> {
        private final List<Long> acquired = new ArrayList<>();
        private final List<Long> released = new ArrayList<>();
        private int held = 0;

        @Override
        public <R> R executeLocked(final Long lockingKey, final Supplier<R> supplier) {
            acquired.add(lockingKey);
            held++;
            try {
                return supplier.get();
            } finally {
                held--;
                released.add(lockingKey);
            }
        }
    }
}
