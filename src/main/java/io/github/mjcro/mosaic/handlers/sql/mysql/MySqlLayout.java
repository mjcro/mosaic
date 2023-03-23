package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.exceptions.IllegalDatabaseEntityNameException;
import io.github.mjcro.mosaic.handlers.sql.Layout;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Base class for MySQL layouts.
 */
public abstract class MySqlLayout implements Layout {
    private final boolean detectTransaction;
    protected final String columnLinkId;
    protected final String columnTypeId;

    /**
     * Base MySQL layout.
     *
     * @param detectTransaction If true, layout will detect transactional context and insert "FOR UPDATE"
     *                          while reading data.
     * @param columnLinkId      Column name to store link identifier.
     * @param columnTypeId      Column name to store type identifier.
     */
    protected MySqlLayout(boolean detectTransaction, String columnLinkId, String columnTypeId) {
        this.detectTransaction = detectTransaction;
        this.columnLinkId = Objects.requireNonNull(columnLinkId, "columnLinkId");
        this.columnTypeId = Objects.requireNonNull(columnTypeId, "columnTypeId");
    }


    /**
     * True if connection has transactional context.
     *
     * @param connection Connection to analyze.
     * @return True if transaction is active, false otherwise.
     * @throws SQLException On database error.
     */
    protected boolean insideTransaction(Connection connection) throws SQLException {
        return detectTransaction && !connection.getAutoCommit();
    }

    /**
     * Escapes database table/column name.
     *
     * @param name Name to escape.
     * @return Escaped name.
     * @throws IllegalDatabaseEntityNameException If invalid name given.
     */
    protected String escapeName(String name) throws IllegalDatabaseEntityNameException {
        if (name != null && !name.isEmpty()) {
            if (name.startsWith("`")) {
                if (name.endsWith("`")) {
                    return name;
                }
            } else if (!name.endsWith("`")) {
                return "`" + name + "`";
            }
        }

        throw new IllegalDatabaseEntityNameException(name);
    }
}
