package io.github.mjcro.mosaic.handlers.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface Mapper {
    /**
     * @return Table common name.
     */
    String getCommonName();

    /**
     * @return Column names.
     */
    String[] getColumnNames();

    /**
     * Set placeholder values into given statement.
     *
     * @param stmt   Statement to set placeholders into.
     * @param offset Starting offset.
     * @param value  Source value.
     * @throws SQLException On error.
     */
    void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException;

    /**
     * Reads value from result set.
     *
     * @param resultSet Result set with data.
     * @param offset    Starting offset.
     * @return Value.
     * @throws SQLException On error.
     */
    Object readObjectValue(ResultSet resultSet, int offset) throws SQLException;
}
