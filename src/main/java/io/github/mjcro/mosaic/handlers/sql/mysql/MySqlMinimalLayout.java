package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Minimal layout to store data.
 * <p>
 * Contains following columns:
 * - id - incremental, int64
 * - linkId - identifier of anchor link
 * - typeId - identifier of type
 * - ... - value column(s), names are provided by mapper
 * <p>
 * Whenever data is requested to be deleted, it will be actually deleted from
 * database.
 */
public class MySqlMinimalLayout extends MySqlBasicLayout {
    public static final MySqlMinimalLayout DEFAULT = new MySqlMinimalLayout(
            true,
            "linkId",
            "typeId"
    );

    /**
     * Constructs minimal MySQL layout with given column names.
     *
     * @param detectTransaction If true, layout will detect transactional context and insert "FOR UPDATE"
     *                          while reading data.
     * @param columnLinkId      Column name to store link identifier.
     * @param columnTypeId      Column name to store type identifier.
     */
    public MySqlMinimalLayout(boolean detectTransaction, String columnLinkId, String columnTypeId) {
        super(detectTransaction, columnLinkId, columnTypeId);
    }

    @Override
    public <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(
            Mapper mapper,
            Connection connection,
            String tableName,
            Collection<Long> linkIds,
            Collection<Key> keys
    ) throws SQLException {
        return findByLinkId0(
                mapper,
                connection,
                tableName,
                linkIds,
                keys,
                null
        );
    }

    @Override
    public void store(
            Mapper mapper,
            Connection connection,
            String tableName,
            long linkId,
            Map<? extends KeySpec, List<Object>> values
    ) throws SQLException {
        store0(
                mapper,
                connection,
                tableName,
                linkId,
                values
        );
    }

    @Override
    public void delete(
            Connection connection,
            String tableName,
            long linkId,
            Collection<? extends KeySpec> keys
    ) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(escapeName(tableName));
        appendWhereAndExecute(sb, connection, linkId, keys);
    }
}
