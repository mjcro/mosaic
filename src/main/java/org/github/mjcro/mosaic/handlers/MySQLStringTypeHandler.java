package org.github.mjcro.mosaic.handlers;

import org.github.mjcro.mosaic.KeySpec;
import org.github.mjcro.mosaic.TypeHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySQLStringTypeHandler implements TypeHandler {
    public String getTableName(final String tablePrefix) {
        return "`" + tablePrefix + "String`";
    }

    @Override
    public void create(
            final Connection connection,
            final String tablePrefix,
            final long id,
            final Map<KeySpec, List<Object>> values
    ) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(getTableName(tablePrefix)).append(" (`linkId`,`typeId`,`value`) VALUES ");
        boolean first = true;
        for (Map.Entry<KeySpec, List<Object>> entry : values.entrySet()) {
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append("(?,?,?)");
            }
        }

        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (Map.Entry<KeySpec, List<Object>> entry : values.entrySet()) {
                for (final Object value : entry.getValue()) {
                    if (!(value instanceof CharSequence)) {
                        throw new SQLException("Given value not String");
                    }
                    stmt.setLong(offset++, id);
                    stmt.setInt(offset++, entry.getKey().getTypeId());
                    stmt.setString(offset++, value.toString());
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
        sb.append("SELECT `linkId`,`typeId`,`value` FROM ").append(getTableName(tablePrefix)).append(" WHERE");

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
                        String value = rs.getString(3);

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
    public void update(
            final Connection connection,
            final String tablePrefix,
            final long id,
            final Map<KeySpec, List<Object>> values
    ) throws SQLException {

    }
}
