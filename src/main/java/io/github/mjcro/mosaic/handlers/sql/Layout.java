package io.github.mjcro.mosaic.handlers.sql;

import io.github.mjcro.mosaic.KeySpec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines behavior database should read and modify data.
 */
public interface Layout {
    /**
     * Finds data linked to identifiers.
     *
     * @param mapper     Mapper to use while reading data.
     * @param connection Database connection.
     * @param tableName  Database table name.
     * @param linkIds    Identifiers collection.
     * @param keys       Keys to read from database.
     * @return Data, grouped by identifiers.
     * @throws SQLException On database error.
     */
    <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(
            Mapper mapper,
            Connection connection,
            String tableName,
            Collection<Long> linkIds,
            Collection<Key> keys
    ) throws SQLException;

    /**
     * Stores (creates or updates) data into database.
     *
     * @param mapper     Mapper to use while writing data.
     * @param connection Database connection.
     * @param tableName  Database table name.
     * @param linkId     Link identifier.
     * @param values     Values to store.
     * @throws SQLException On database error.
     */
    void store(
            Mapper mapper,
            Connection connection,
            String tableName,
            long linkId,
            Map<? extends KeySpec, List<Object>> values
    ) throws SQLException;

    /**
     * Deletes data from database.
     *
     * @param connection Database connection.
     * @param tableName  Database table name.
     * @param linkId     Link identifier.
     * @param keys       Keys to delete.
     * @throws SQLException On database error.
     */
    void delete(
            Connection connection,
            String tableName,
            long linkId,
            Collection<? extends KeySpec> keys
    ) throws SQLException;
}
