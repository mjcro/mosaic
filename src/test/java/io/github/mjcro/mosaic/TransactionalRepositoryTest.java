package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.handlers.sql.mappers.BigDecimalMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.LongMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithCreationTimeSeconds;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;

public class TransactionalRepositoryTest extends BaseRepositoryTest {
    @Test
    public void testAll() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:mosaic1;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'");

        // Initializing data provider
        TransactionalRepository<Key> repository = new TransactionalRepository<>(
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.DEFAULT, new StringMapper())
                        .with(Long.class, MySqlMinimalLayout.DEFAULT, new LongMapper())
                        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper())
                        .with(BigDecimal.class, MySqlMinimalLayout.DEFAULT, new BigDecimalMapper().withCommonName("Discount"))
                        .with(Amount.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new CustomAmountMapper()),
                Key.class,
                "unitTest"
        );

        // Reading non-existing entity
        Assert.assertTrue(repository.findById(connection, 8).isEmpty());

        // Creating entity
        Map<Key, List<Object>> entity1 = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "John")
                .putSingle(Key.LAST_NAME, "Smith")
                .putSingle(Key.PRICING_PLAN, 1237154245178L)
                .putSingle(Key.CREATED_AT, Instant.parse("2021-07-14T06:07:08Z"))
                .putSingle(Key.DISCOUNT_PERCENT, BigDecimal.valueOf(0.5))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.TEN))
                .build();
        repository.store(connection, 8, entity1);

        // Reading created entity
        Map<Key, List<Object>> read = repository.findById(connection, 8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Partial read single key
        read = repository.findById(connection, 8, Collections.singleton(Key.LAST_NAME));
        Assert.assertFalse(read.isEmpty());
        Assert.assertEquals(read.size(), 1);
        Assert.assertTrue(read.containsKey(Key.LAST_NAME));
        Assert.assertFalse(read.get(Key.LAST_NAME).isEmpty());
        Assert.assertEquals(read.get(Key.LAST_NAME).get(0), "Smith");

        // Partial read several keys
        read = repository.findById(connection, 8, Arrays.asList(Key.LAST_NAME, Key.DISCOUNT_PERCENT));
        Assert.assertFalse(read.isEmpty());
        Assert.assertEquals(read.size(), 2);
        Assert.assertTrue(read.containsKey(Key.LAST_NAME));
        Assert.assertFalse(read.get(Key.LAST_NAME).isEmpty());
        Assert.assertEquals(read.get(Key.LAST_NAME).get(0), "Smith");
        Assert.assertTrue(read.containsKey(Key.DISCOUNT_PERCENT));
        Assert.assertFalse(read.get(Key.DISCOUNT_PERCENT).isEmpty());
        Assert.assertEquals(read.get(Key.DISCOUNT_PERCENT).get(0), BigDecimal.valueOf(0.5));

        // Performing partial delete
        repository.delete(connection, 8, Collections.singleton(Key.LAST_NAME));
        entity1.remove(Key.LAST_NAME);
        read = repository.findById(connection, 8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Updating
        Map<Key, List<Object>> update = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.LAST_NAME, "Nobody")
                .putSingle(Key.UPDATED_AT, Instant.parse("2021-07-16T06:07:03Z"))
                .putSingle(Key.EXPIRY_AT, Instant.parse("2025-01-01T00:00:00Z"))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.ONE))
                .build();
        repository.store(connection, 8, update);
        entity1.putAll(update);
        read = repository.findById(connection, 8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Performing full delete
        repository.delete(connection, 8);
        read = repository.findById(connection, 8);
        Assert.assertTrue(read.isEmpty());
    }
}