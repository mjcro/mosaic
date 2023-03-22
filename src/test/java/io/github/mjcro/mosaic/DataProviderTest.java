package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;
import io.github.mjcro.mosaic.handlers.sql.Mapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
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

public class DataProviderTest {
    @Test
    public void testAll() throws SQLException {
        // Creating schema
        DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/test/resources/dataProviderTest.sql'");
        // Initializing data provider
        DataProvider<Key> provider = new DataProvider<>(
                () -> DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"),
                new TypeHandlerResolverMap()
                        .with(String.class, MySqlMinimalLayout.INSTANCE, new StringMapper())
                        .with(Instant.class, MySqlMinimalLayout.INSTANCE, new InstantSecondsMapper())
                        .with(Amount.class, MySqlMinimalLayout.INSTANCE, new CustomAmountMapper())
        );

        // Reading non-existing entity
        Assert.assertTrue(provider.findById(Key.class, "dataProviderUnit", Collections.singleton(8L)).isEmpty());

        // Creating entity
        Map<Key, List<Object>> entity1 = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "John")
                .putSingle(Key.LAST_NAME, "Smith")
                .putSingle(Key.CREATED_AT, Instant.parse("2021-07-14T06:07:08Z"))
                .putSingle(Key.ACCOUNT_BALANCE, new Amount(Currency.getInstance("USD"), BigDecimal.TEN))
                .build();
        provider.store("dataProviderUnit", 8, entity1);

        // Reading created entity
        List<Entity<Key>> read = provider.findById(Key.class, "dataProviderUnit", Collections.singleton(8L));
        Assert.assertEquals(read.size(), 1);
    }

    private void assertResultEquals() {
    }

    public enum Key implements KeySpec {
        FIRST_NAME(1, String.class),
        LAST_NAME(2, String.class),

        CREATED_AT(11, Instant.class),
        EXPIRY_AT(11, Instant.class),

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