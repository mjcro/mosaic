package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.exceptions.UnexpectedValueException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringMapper extends AbstractSingleColumnValueMapper {
    @Override
    public String getCommonName() {
        return "String";
    }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        if (value instanceof CharSequence) {
            stmt.setString(offset, value.toString());
        } else {
            throw new UnexpectedValueException(value);
        }
    }

    @Override
    public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return resultSet.getString(offset);
    }
}
