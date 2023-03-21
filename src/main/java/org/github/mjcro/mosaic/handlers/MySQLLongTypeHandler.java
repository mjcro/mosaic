package org.github.mjcro.mosaic.handlers;

import org.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLLongTypeHandler extends MySQLAbstractTypeHandler {
    public MySQLLongTypeHandler() {
        super("Long", "`value`");
    }

    @Override
    protected void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof Long) {
            stmt.setLong(offset, (Long) value);
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    protected Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return resultSet.getLong(offset);
    }
}
