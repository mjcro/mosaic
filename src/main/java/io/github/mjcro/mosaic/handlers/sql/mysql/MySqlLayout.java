package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.handlers.sql.Layout;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for MySQL layouts.
 */
public abstract class MySqlLayout implements Layout {
    /**
     * True if connection has transactional context.
     *
     * @param connection Connection to analyze.
     * @return True if transaction is active, false otherwise.
     * @throws SQLException On database error.
     */
    protected boolean insideTransaction(Connection connection) throws SQLException {
        return !connection.getAutoCommit();
    }
}
