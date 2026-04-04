package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.handlers.sql.mappers.BigDecimalMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.LongMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithCreationTimeSeconds;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;

public class RepositoriesTest extends BaseRepositoryTest {
    @Test
    public void testRepository() throws SQLException {
        // Creating schema
        DriverManager.getConnection("jdbc:h2:mem:mosaic1;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'");
        // Initializing data provider
        Repository<Key> repository = new Repository<>(
                () -> DriverManager.getConnection("jdbc:h2:mem:mosaic1;DB_CLOSE_DELAY=-1"),
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.DEFAULT, new StringMapper())
                        .with(Long.class, MySqlMinimalLayout.DEFAULT, new LongMapper())
                        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper())
                        .with(BigDecimal.class, MySqlMinimalLayout.DEFAULT, new BigDecimalMapper().withCommonName("Discount"))
                        .with(Amount.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new CustomAmountMapper()),
                Key.class,
                "unitTest"
        );

        doTest(repository);
    }

    @Test
    public void testParallelRepository() throws SQLException, InterruptedException {
        // Initializing executor
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Creating schema
        DriverManager.getConnection("jdbc:h2:mem:mosaic2;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'");
        // Initializing data provider
        ParallelRepository<Key> repository = new ParallelRepository<>(
                executorService,
                () -> DriverManager.getConnection("jdbc:h2:mem:mosaic2;DB_CLOSE_DELAY=-1"),
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.DEFAULT, new StringMapper())
                        .with(Long.class, MySqlMinimalLayout.DEFAULT, new LongMapper())
                        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper())
                        .with(BigDecimal.class, MySqlMinimalLayout.DEFAULT, new BigDecimalMapper().withCommonName("Discount"))
                        .with(Amount.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new CustomAmountMapper()),
                Key.class,
                "unitTest"
        );

        doTest(repository);

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    private void doTest(AbstractConnectionProviderRepository<Key> repository) throws SQLException {
        // Reading non-existing entity
        Assertions.assertTrue(repository.findById(8).isEmpty());

        // Creating entity
        Map<Key, List<Object>> entity1 = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "John")
                .putSingle(Key.LAST_NAME, "Smith")
                .putSingle(Key.PRICING_PLAN, 1237154245178L)
                .putSingle(Key.CREATED_AT, Instant.parse("2021-07-14T06:07:08Z"))
                .putSingle(Key.DISCOUNT_PERCENT, BigDecimal.valueOf(0.5))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.TEN))
                .build();
        repository.store(8, entity1);

        // Reading created entity
        Map<Key, List<Object>> read = repository.findById(8);
        Assertions.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Partial read single key
        read = repository.findById(8, Collections.singleton(Key.LAST_NAME));
        Assertions.assertFalse(read.isEmpty());
        Assertions.assertEquals(1, read.size());
        Assertions.assertTrue(read.containsKey(Key.LAST_NAME));
        Assertions.assertFalse(read.get(Key.LAST_NAME).isEmpty());
        Assertions.assertEquals("Smith", read.get(Key.LAST_NAME).get(0));

        // Partial read several keys
        read = repository.findById(8, Arrays.asList(Key.LAST_NAME, Key.DISCOUNT_PERCENT));
        Assertions.assertFalse(read.isEmpty());
        Assertions.assertEquals(2, read.size());
        Assertions.assertTrue(read.containsKey(Key.LAST_NAME));
        Assertions.assertFalse(read.get(Key.LAST_NAME).isEmpty());
        Assertions.assertEquals("Smith", read.get(Key.LAST_NAME).get(0));
        Assertions.assertTrue(read.containsKey(Key.DISCOUNT_PERCENT));
        Assertions.assertFalse(read.get(Key.DISCOUNT_PERCENT).isEmpty());
        Assertions.assertEquals(BigDecimal.valueOf(0.5), read.get(Key.DISCOUNT_PERCENT).get(0));

        // Performing partial delete
        repository.delete(8, Collections.singleton(Key.LAST_NAME));
        entity1.remove(Key.LAST_NAME);
        read = repository.findById(8);
        Assertions.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Updating
        Map<Key, List<Object>> update = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.LAST_NAME, "Nobody")
                .putSingle(Key.UPDATED_AT, Instant.parse("2021-07-16T06:07:03Z"))
                .putSingle(Key.EXPIRY_AT, Instant.parse("2025-01-01T00:00:00Z"))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.ONE))
                .build();
        repository.store(8, update);
        entity1.putAll(update);
        read = repository.findById(8);
        Assertions.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Performing full delete
        repository.delete(8);
        read = repository.findById(8);
        Assertions.assertTrue(read.isEmpty());
    }
}
