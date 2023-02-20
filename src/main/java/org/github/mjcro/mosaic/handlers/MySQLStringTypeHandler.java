package org.github.mjcro.mosaic.handlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLStringTypeHandler extends MySQLAbstractTypeHandler {
    public MySQLStringTypeHandler() {
        super("String", "`value`");
    }

    @Override
    protected void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof CharSequence) {
            stmt.setString(offset, value.toString());
        } else {
            throw new SQLException("Expected string"); // TODO add exception for this and handle nulls
        }
    }

    @Override
    protected Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return resultSet.getString(offset);
    }
}
