package io.github.mjcro.mosaic.handlers;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class MySQLInstantSecondsTypeHandler extends MySQLAbstractTypeHandler {
    public MySQLInstantSecondsTypeHandler() {
        super("Instant", "`value`");
    }

    @Override
    protected void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Instant) {
            stmt.setLong(offset, ((Instant) value).getEpochSecond());
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    protected Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return Instant.ofEpochSecond(resultSet.getLong(offset));
    }
}
