package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalMapper extends AbstractSingleColumnValueMapper {
    @Override
    public String getCommonName() {
        return "BigDecimal";
    }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof BigDecimal) {
            stmt.setBigDecimal(offset, (BigDecimal) value);
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return resultSet.getBigDecimal(offset);
    }
}
