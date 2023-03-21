package org.github.mjcro.mosaic.example.mosaic;

import org.github.mjcro.mosaic.example.domain.Amount;
import org.github.mjcro.mosaic.handlers.MySQLAbstractTypeHandler;
import org.github.mjcro.mosaic.handlers.UnexpectedValueException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;

public class MySQLAmountTypeHandler extends MySQLAbstractTypeHandler {
    public MySQLAmountTypeHandler() {
        super("Amount", "`currencyCode`, `amount`");
    }

    @Override
    protected void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Amount) {
            Amount a = (Amount) value;
            stmt.setString(offset, a.getCurrency().getCurrencyCode());
            stmt.setBigDecimal(offset + 1, a.getValue());
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    protected Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        String currencyCode = resultSet.getString(offset);
        BigDecimal amount = resultSet.getBigDecimal(offset + 1);
        return new Amount(Currency.getInstance(currencyCode), amount);
    }
}
