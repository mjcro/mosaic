package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class InstantMillisMapper extends AbstractSingleColumnValueMapper {
    @Override
    public String getCommonName() {
        return "Instant";
    }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Instant) {
            stmt.setLong(offset, ((Instant) value).toEpochMilli());
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return Instant.ofEpochMilli(resultSet.getLong(offset));
    }
}
