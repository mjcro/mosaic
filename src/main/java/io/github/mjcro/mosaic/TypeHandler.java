package io.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Defines components, able to operate (read and write) with database.
 */
public interface TypeHandler {
    /**
     * Fetches data from database.
     *
     * @param connection  Database connection to use.
     * @param tablePrefix Database table prefix.
     * @param linkIds     Anchor link identifiers.
     * @param keys        Keys to read.
     * @return Matched records.
     * @throws SQLException On database error.
     */
    <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(
            Connection connection,
            String tablePrefix,
            Collection<Long> linkIds,
            Collection<Key> keys
    ) throws SQLException;

    /**
     * Fetches data from database.
     *
     * @param connection  Database connection to use.
     * @param tablePrefix Database table prefix.
     * @param linkId      Anchor link identifier.
     * @param keys        Keys to read.
     * @return Matched records.
     * @throws SQLException On database error.
     */
    default <Key extends KeySpec> Map<Key, List<Object>> findByLinkId(
            Connection connection,
            String tablePrefix,
            long linkId,
            Collection<Key> keys
    ) throws SQLException {
        Map<Long, Map<Key, List<Object>>> data = findByLinkId(
                connection, tablePrefix, Collections.singleton(linkId), keys
        );
        if (data.isEmpty()) {
            return Collections.emptyMap();
        }
        return data.get(linkId);
    }

    /**
     * Stores data.
     *
     * @param connection  Database connection to use.
     * @param tablePrefix Database table prefix.
     * @param linkId      Anchor link identifier.
     * @param values      Values to store.
     * @throws SQLException On database error.
     */
    void store(
            Connection connection,
            String tablePrefix,
            long linkId,
            Map<? extends KeySpec, List<Object>> values
    ) throws SQLException;

    /**
     * Deletes data.
     *
     * @param connection  Database connection to use.
     * @param tablePrefix Database table prefix.
     * @param linkId      Anchor link identifier.
     * @param keys        Keys to delete.
     * @throws SQLException On database error.
     */
    <Key extends KeySpec> void delete(
            Connection connection,
            String tablePrefix,
            long linkId,
            Collection<Key> keys
    ) throws SQLException;
}
