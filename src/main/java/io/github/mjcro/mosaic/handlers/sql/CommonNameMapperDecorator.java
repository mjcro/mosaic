package io.github.mjcro.mosaic.handlers.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Decorator that replaces common table name.
 */
class CommonNameMapperDecorator implements Mapper {
    private final Mapper decorated;
    private final String commonName;

    CommonNameMapperDecorator(Mapper decorated, String commonName) {
        this.decorated = Objects.requireNonNull(decorated, "decorated");
        this.commonName = Objects.requireNonNull(commonName, "commonName");
    }

    public Mapper getDecorated() {
        return decorated;
    }

    @Override
    public String getCommonName() {
        return commonName;
    }

    @Override
    public String[] getColumnNames() {
        return getDecorated().getColumnNames();
    }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        getDecorated().setPlaceholdersValue(stmt, offset, value);
    }

    @Override
    public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
        return getDecorated().readObjectValue(resultSet, offset);
    }
}
