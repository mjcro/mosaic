package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
public class MySqlMinimalLayout extends MySqlLayout {
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
        HashMap<Integer, Key> reverseMap = new HashMap<>();
        for (final Key key : keys) {
            reverseMap.put(key.getTypeId(), key);
        }

        String[] columns = mapper.getColumnNames();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(escapeName(columnLinkId)).append(",").append(escapeName(columnTypeId));
        for (String column : columns) {
            sb.append(",").append(escapeName(column));
        }

        sb.append(" FROM ").append(escapeName(tableName)).append(" WHERE");
        sb.append(" ").append(escapeName(columnLinkId)).append(" IN (");
        for (int i = 0; i < linkIds.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        sb.append(" AND ").append(escapeName(columnTypeId)).append(" IN (");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        if (insideTransaction(connection)) {
            // Inside transaction
            sb.append(" FOR UPDATE");
        }

        Map<Long, Map<Key, List<Object>>> response = new HashMap<>();
        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (final Long id : linkIds) {
                stmt.setLong(offset++, id);
            }
            for (final Key key : keys) {
                stmt.setInt(offset++, key.getTypeId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.isBeforeFirst()) {
                    while (rs.next()) {
                        if (rs.isAfterLast()) {
                            break;
                        }

                        long linkId = rs.getLong(1);
                        Key key = reverseMap.get(rs.getInt(2));
                        if (key == null) {
                            // Key not resolved
                            continue;
                        }
                        Object value = mapper.readObjectValue(rs, 3);

                        if (!response.containsKey(linkId)) {
                            response.put(linkId, new HashMap<>());
                        }

                        Map<Key, List<Object>> subMap = response.get(linkId);
                        if (!subMap.containsKey(key)) {
                            subMap.put(key, new ArrayList<>());
                        }
                        subMap.get(key).add(value);
                    }
                }
            }
        }

        return response;
    }

    @Override
    public void store(
            Mapper mapper,
            Connection connection,
            String tableName,
            long linkId,
            Map<? extends KeySpec, List<Object>> values
    ) throws SQLException {
        // Deleting previous values
        delete(connection, tableName, linkId, values.keySet());

        String[] columns = mapper.getColumnNames();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(escapeName(tableName));
        sb.append(" (").append(escapeName(columnLinkId)).append(",").append(escapeName(columnTypeId));
        for (String column : columns) {
            sb.append(",").append(escapeName(column));
        }
        sb.append(") VALUES ");
        boolean first = true;
        for (Map.Entry<? extends KeySpec, List<Object>> entry : values.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append("(?,?,");
                for (int j = 0; j < columns.length; j++) {
                    if (j > 0) {
                        sb.append(",");
                    }
                    sb.append("?");
                }
                sb.append(")");
            }
        }

        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (Map.Entry<? extends KeySpec, List<Object>> entry : values.entrySet()) {
                for (final Object value : entry.getValue()) {
                    stmt.setLong(offset++, linkId);
                    stmt.setInt(offset++, entry.getKey().getTypeId());
                    mapper.setPlaceholdersValue(stmt, offset, value);
                    offset += columns.length;
                }
            }

            stmt.executeUpdate();
        }
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
        sb.append(" WHERE ").append(escapeName(columnLinkId)).append(" = ? AND ");
        sb.append(escapeName(columnTypeId)).append(" IN (");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            stmt.setLong(offset++, linkId);
            for (KeySpec key : keys) {
                stmt.setInt(offset++, key.getTypeId());
            }

            stmt.executeUpdate();
        }
    }
}
