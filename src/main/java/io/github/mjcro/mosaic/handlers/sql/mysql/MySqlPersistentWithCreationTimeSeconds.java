package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySqlPersistentWithCreationTimeSeconds extends MySqlLayout {
    public static final MySqlPersistentWithCreationTimeSeconds INSTANCE = new MySqlPersistentWithCreationTimeSeconds();

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
        sb.append("SELECT `linkId`,`typeId`");
        for (String column : columns) {
            sb.append(",").append(column);
        }
        sb.append(" FROM ").append(tableName).append(" WHERE `active`=1 AND");

        sb.append(" `linkId` IN (");
        for (int i = 0; i < linkIds.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");

        sb.append(" AND `typeId` IN (");
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

        // Current time
        long currentTime = Instant.now().getEpochSecond();

        String[] columns = mapper.getColumnNames();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName);
        sb.append(" (`linkId`,`typeId`,`active`,`time`");
        for (String column : columns) {
            sb.append(",").append(column);
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
                sb.append("(?,?,?,?,");
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
                    stmt.setInt(offset++, 1);
                    stmt.setLong(offset++, currentTime);
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
        sb.append("UPDATE ").append(tableName).append(" SET `active`=0 WHERE `linkId`=? AND `typeId` IN (");
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
