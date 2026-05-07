package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Persistent (no physical data deletion) layout that tracks both creation and modification time
 * and skips writes when the new value equals the value already stored.
 * <p>
 * Contains following columns:
 * - id - incremental, int64
 * - linkId - identifier of anchor link
 * - typeId - identifier of type
 * - active - 1 if record is active, 0 is considered deleted
 * - created - Unix timestamp in seconds when the record was inserted
 * - modified - Unix timestamp in seconds of the last activity flag change (deletion)
 * - ... - value column(s), names are provided by mapper
 * <p>
 * On store, single-valued entries equal to existing active values are skipped to avoid
 * redundant inserts. Multi-valued entries are always written. Deletion is performed by setting
 * the activity flag to 0 and updating the modification timestamp; rows are never removed.
 */
public class MySqlPersistentWithChangesAndCreationModificationTimeSeconds extends MySqlBasicLayout {
    public static final MySqlPersistentWithChangesAndCreationModificationTimeSeconds DEFAULT = new MySqlPersistentWithChangesAndCreationModificationTimeSeconds(
            true,
            "linkId",
            "typeId",
            "active",
            "created",
            "modified"
    );

    protected final String columnIsActive;
    protected final String columnCreationTime;
    protected final String columnModificationTime;

    /**
     * Constructs persistent MySQL layout with given column names.
     *
     * @param detectTransaction      If true, layout will detect transactional context and insert "FOR UPDATE"
     *                               while reading data.
     * @param columnLinkId           Column name to store link identifier. Not nullable.
     * @param columnTypeId           Column name to store type identifier. Not nullable.
     * @param columnIsActive         Column name to store boolean 0/1 activity flag. Not nullable.
     * @param columnCreationTime     Column name to store creation timestamp. Not nullable.
     * @param columnModificationTime Column name to store last modification timestamp. Not nullable.
     */
    public MySqlPersistentWithChangesAndCreationModificationTimeSeconds(
            boolean detectTransaction,
            String columnLinkId,
            String columnTypeId,
            String columnIsActive,
            String columnCreationTime,
            String columnModificationTime
    ) {
        super(detectTransaction, columnLinkId, columnTypeId);
        this.columnIsActive = Objects.requireNonNull(columnIsActive, "columnIsActive");
        this.columnCreationTime = Objects.requireNonNull(columnCreationTime, "columnCreationTime");
        this.columnModificationTime = Objects.requireNonNull(columnModificationTime, "columnModificationTime");
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
            Map<? extends KeySpec, List<Object>> values0
    ) throws SQLException {
        if (values0 == null || values0.isEmpty()) {
            return;
        }
        Map<? extends KeySpec, List<Object>> values;

        // Searching for existing values
        Map<? extends KeySpec, Object> existing = findByLinkId(
                mapper,
                connection,
                tableName,
                Collections.singleton(linkId),
                values0.keySet()
        )
                .getOrDefault(linkId, Collections.emptyMap())
                .entrySet()
                .stream()
                .filter($ -> $.getValue().size() == 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        $ -> $.getValue().get(0)
                ));

        values = values0.entrySet()
                .stream()
                .filter($entry -> {
                    if ($entry.getValue().size() != 1) {
                        // Skipping list values to simplify check
                        return true;
                    }
                    // Value to store equals to one already existing in database
                    return !Objects.equals(existing.get($entry.getKey()), $entry.getValue().get(0));
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));

        if (values.isEmpty()) {
            return;
        }

        long currentTime = Instant.now().getEpochSecond();
        store0(
                mapper,
                connection,
                tableName,
                linkId,
                values,
                new AdditionalColumn(columnIsActive, (stmt, offset) -> stmt.setInt(offset, 1)),
                new AdditionalColumn(columnCreationTime, (stmt, offset) -> stmt.setLong(offset, currentTime)),
                new AdditionalColumn(columnModificationTime, (stmt, offset) -> stmt.setLong(offset, currentTime))
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
        sb.append("UPDATE ").append(tableName).append(" SET ")
                .append(escapeName(columnIsActive)).append("=0,")
                .append(escapeName(columnModificationTime)).append("=").append(Instant.now().getEpochSecond());
        appendWhereAndExecute(sb, connection, linkId, null, keys);
    }
}
