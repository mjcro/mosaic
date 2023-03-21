package org.github.mjcro.mosaic.handlers;

import org.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class MySQLInstantMillisTypeHandler extends MySQLAbstractTypeHandler {
    public MySQLInstantMillisTypeHandler() {
        super("Instant", "`value`");
    }

    @Override
    protected void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Instant) {
            stmt.setLong(offset, ((Instant) value).toEpochMilli());
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    protected Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return Instant.ofEpochMilli(resultSet.getLong(offset));
    }
}
