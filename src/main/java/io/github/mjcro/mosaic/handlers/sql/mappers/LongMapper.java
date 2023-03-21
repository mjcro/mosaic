package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LongMapper extends AbstractSingleColumnValueMapper {
    @Override
    public String getCommonName() {
        return "Long";
    }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Long) {
            stmt.setLong(offset, (Long) value);
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return resultSet.getLong(offset);
    }
}
