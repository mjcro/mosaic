package org.github.mjcro.mosaic.handlers;

import org.github.mjcro.mosaic.KeySpec;
import org.github.mjcro.mosaic.TypeHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class MySQLAbstractTypeHandler implements TypeHandler {
    private final String commonTableName;
    private final String[] valueColumns;

    protected MySQLAbstractTypeHandler(
            String commonTableName,
            String... valueColumns
    ) {
        this.commonTableName = commonTableName;
        this.valueColumns = valueColumns;
    }

    public String getTableName(final String tablePrefix) {
        return "`" + tablePrefix + commonTableName;
    }

    protected abstract void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException;

    protected abstract Object readObjectValue(ResultSet resultSet, int offset) throws SQLException;

    @Override
    public void create(
            final Connection connection,
            final String tablePrefix,
            final long id,
            final Map<KeySpec, List<Object>> values
    ) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(getTableName(tablePrefix));
        sb.append(" (`linkId`,`typeId`,");
        for (String column : valueColumns) {
            sb.append(",").append(column);
        }
        sb.append(") VALUES ");
        boolean first = true;
        for (Map.Entry<KeySpec, List<Object>> entry : values.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append("(?,?,");
                for (int j = 0; j < valueColumns.length; j++) {
                    sb.append("?");
                }
                sb.append(")");
            }
        }

        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (Map.Entry<KeySpec, List<Object>> entry : values.entrySet()) {
                for (final Object value : entry.getValue()) {
                    stmt.setLong(offset++, id);
                    stmt.setInt(offset++, entry.getKey().getTypeId());
                    setPlaceholdersValue(stmt, offset, value);
                    offset += valueColumns.length;
                }
            }

            stmt.executeUpdate();
        }
    }

    @Override
    public <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findById(
            final Connection connection,
            final String tablePrefix,
            final Collection<Long> ids,
            final Collection<Key> keys
    ) throws SQLException {
        HashMap<Integer, Key> reverseMap = new HashMap<>();
        for (final Key key : keys) {
            reverseMap.put(key.getTypeId(), key);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `linkId`,`typeId`");
        for (String column : valueColumns) {
            sb.append(",").append(column);
        }
        sb.append(" FROM").append(getTableName(tablePrefix)).append(" WHERE");

        sb.append(" `linkId` IN (");
        for (int i = 0; i < ids.size(); i++) {
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

        Map<Long, Map<Key, List<Object>>> response = new HashMap<>();
        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (final Long id : ids) {
                stmt.setLong(offset++, id);
            }
            for (final Key key : keys) {
                stmt.setInt(offset++, key.getTypeId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    while (rs.next()) {
                        if (rs.isAfterLast()) {
                            break;
                        }

                        long linkId = rs.getLong(1);
                        Key key = reverseMap.get(rs.getInt(2));
                        Object value = readObjectValue(rs, 3);

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
    public <Key> void delete(
            Connection connection,
            String tablePrefix,
            long id,
            Collection<Key> keys
    ) throws SQLException {
        // TODO
    }

    @Override
    public void update(
            Connection connection,
            String tablePrefix,
            long id,
            Map<KeySpec, List<Object>> values
    ) throws SQLException {
        // TODO
    }
}
