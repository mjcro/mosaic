package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;
import io.github.mjcro.mosaic.handlers.sql.Mapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithCreationTimeSeconds;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RepositoryTest {
    @Test
    public void testAll() throws SQLException {
        // Creating schema
        DriverManager.getConnection("jdbc:h2:mem:mosaic;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'");
        // Initializing data provider
        Repository<Key> provider = new Repository<>(
                () -> DriverManager.getConnection("jdbc:h2:mem:mosaic;DB_CLOSE_DELAY=-1"),
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.DEFAULT, new StringMapper())
                        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper())
                        .with(Amount.class, MySqlPersistentWithCreationTimeSeconds.INSTANCE, new CustomAmountMapper()),
                Key.class,
                "unitTest"
        );

        // Reading non-existing entity
        Assert.assertTrue(provider.findById(8).isEmpty());

        // Creating entity
        Map<Key, List<Object>> entity1 = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "John")
                .putSingle(Key.LAST_NAME, "Smith")
                .putSingle(Key.CREATED_AT, Instant.parse("2021-07-14T06:07:08Z"))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.TEN))
                .build();
        provider.store(8, entity1);

        // Reading created entity
        Map<Key, List<Object>> read = provider.findById(8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Performing partial delete
        provider.delete(8, Collections.singleton(Key.LAST_NAME));
        entity1.remove(Key.LAST_NAME);
        read = provider.findById(8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Updating
        Map<Key, List<Object>> update = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.LAST_NAME, "Nobody")
                .putSingle(Key.UPDATED_AT, Instant.parse("2021-07-16T06:07:03Z"))
                .putSingle(Key.EXPIRY_AT, Instant.parse("2025-01-01T00:00:00Z"))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.ONE))
                .build();
        provider.store(8, update);
        entity1.putAll(update);
        read = provider.findById(8);
        Assert.assertFalse(read.isEmpty());
        assertResultEquals(read, entity1);

        // Performing full delete
        provider.delete(8);
        read = provider.findById(8);
        Assert.assertTrue(read.isEmpty());
    }

    private void assertResultEquals(Map<Key, List<Object>> actual, Map<Key, List<Object>> expected) {
        Assert.assertEquals(actual.size(), expected.size(), "Expected and actual maps has different size");
        for (Key key : expected.keySet()) {
            Assert.assertTrue(actual.containsKey(key), "Actual result map missing key " + key);
            List<Object> actualObjects = actual.get(key);
            List<Object> expectedObjects = expected.get(key);
            Assert.assertEquals(actualObjects.size(), expectedObjects.size(), "Object size not equal for key " + key);
            for (Object expectedObject : expectedObjects) {
                Assert.assertTrue(
                        actualObjects.contains(expectedObject),
                        "For key " + key + " missing expected " + expectedObject
                );
            }
        }
    }

    public enum Key implements KeySpec {
        FIRST_NAME(1, String.class),
        LAST_NAME(2, String.class),

        CREATED_AT(11, Instant.class),
        UPDATED_AT(12, Instant.class),
        EXPIRY_AT(13, Instant.class),

        ACCOUNT_BALANCE(21, Amount.class);

        private final int typeId;
        private final Class<?> dataClass;

        Key(final int typeId, final Class<?> clazz) {
            this.typeId = typeId;
            this.dataClass = clazz;
        }

        @Override
        public int getTypeId() {
            return typeId;
        }

        @Override
        public Class<?> getDataClass() {
            return dataClass;
        }
    }

    public static class Amount {
        private final Currency currency;
        private final BigDecimal value;

        public Amount(final Currency currency, final BigDecimal value) {
            this.currency = Objects.requireNonNull(currency, "currency");
            this.value = Objects.requireNonNull(value, "value");
        }

        public Currency getCurrency() {
            return currency;
        }

        public BigDecimal getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue() + " " + getCurrency().getDisplayName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Amount amount = (Amount) o;
            return currency.equals(amount.currency) && value.compareTo(amount.value) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(currency, value);
        }
    }


    public static class CustomAmountMapper implements Mapper {
        private static final String[] columns = new String[]{"`currencyCode`", "`amount`"};

        @Override
        public String getCommonName() {
            return "Amount";
        }

        @Override
        public String[] getColumnNames() {
            return columns;
        }

        @Override
        public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
            if (value instanceof Amount) {
                Amount a = (Amount) value;
                stmt.setString(offset, a.getCurrency().getCurrencyCode());
                stmt.setBigDecimal(offset + 1, a.getValue());
            } else {
                throw new UnexpectedValueException(value);
            }
        }

        @Override
        public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
            String currencyCode = resultSet.getString(offset);
            BigDecimal amount = resultSet.getBigDecimal(offset + 1);
            return new Amount(Currency.getInstance(currencyCode), amount);
        }
    }
}