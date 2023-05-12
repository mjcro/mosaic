package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent (no data deletion) layout to store data.
 * <p>
 * Contains following columns:
 * - id - incremental, int64
 * - linkId - identifier of anchor link
 * - typeId - identifier of type
 * - active - 1 if record is active, 0 is considered deleted
 * - time - Unix timestamp in seconds this record was added to database.
 * - ... - value column(s), names are provided by mapper
 * <p>
 * Whenever data is requested to be deleted, it will be actually deleted from
 * database.
 */
public class MySqlPersistentWithCreationTimeSeconds extends MySqlBasicLayout {
    public static final MySqlPersistentWithCreationTimeSeconds DEFAULT = new MySqlPersistentWithCreationTimeSeconds(
            true,
            "linkId",
            "typeId",
            "active",
            "time"
    );

    protected final String columnIsActive;
    protected final String columnCreationTime;

    /**
     * Constructs persistent MySQL layout with given column names.
     *
     * @param detectTransaction  If true, layout will detect transactional context and insert "FOR UPDATE"
     *                           while reading data.
     * @param columnLinkId       Column name to store link identifier.
     * @param columnTypeId       Column name to store type identifier.
     * @param columnIsActive     Column name to store boolean 0/1 activity flag.
     * @param columnCreationTime Column name to store creation timestamp in seconds.
     */
    public MySqlPersistentWithCreationTimeSeconds(
            boolean detectTransaction,
            String columnLinkId,
            String columnTypeId,
            String columnIsActive,
            String columnCreationTime
    ) {
        super(detectTransaction, columnLinkId, columnTypeId);
        this.columnIsActive = Objects.requireNonNull(columnIsActive, "columnIsActive");
        this.columnCreationTime = Objects.requireNonNull(columnCreationTime, "columnCreationTime");
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
                sb -> sb.append(escapeName(columnIsActive)).append("=1 AND")
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
        // Current time
        long currentTime = Instant.now().getEpochSecond();

        store0(
                mapper,
                connection,
                tableName,
                linkId,
                values,
                new AdditionalColumn(columnIsActive, (stmt, offset) -> stmt.setInt(offset, 1)),
                new AdditionalColumn(columnCreationTime, (stmt, offset) -> stmt.setLong(offset, currentTime))
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
        sb.append("UPDATE ").append(tableName).append(" SET ").append(escapeName(columnIsActive)).append("=0");
        appendWhereAndExecute(sb, connection, linkId, keys);
    }
}
