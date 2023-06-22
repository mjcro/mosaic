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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Basic utility MySQL layout.
 */
public abstract class MySqlBasicLayout extends MySqlLayout {
    /**
     * Constructs basic MySQL layout.
     *
     * @param detectTransaction If true, layout will detect transactional context and insert "FOR UPDATE"
     *                          while reading data.
     * @param columnLinkId      Column name to store link identifier.
     * @param columnTypeId      Column name to store type identifier.
     */
    public MySqlBasicLayout(boolean detectTransaction, String columnLinkId, String columnTypeId) {
        super(detectTransaction, columnLinkId, columnTypeId);
    }

    /**
     * Actually performs data read.
     *
     * @param mapper      Entity data mapper.
     * @param connection  Database connection.
     * @param tableName   Database table name.
     * @param linkIds     Link identifiers to read.
     * @param keys        Keys to read.
     * @param injectWhere Additional condition to inject into WHERE block.
     * @return Found data.
     * @throws SQLException On database error.
     */
    protected <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId0(
            Mapper mapper,
            Connection connection,
            String tableName,
            Collection<Long> linkIds,
            Collection<Key> keys,
            WhereClauseInjector injectWhere
    ) throws SQLException {
        HashMap<Integer, Key> reverseMap = new HashMap<>();
        for (Key key : keys) {
            reverseMap.put(key.getTypeId(), key);
        }

        String[] columns = mapper.getColumnNames();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(escapeName(columnLinkId)).append(",").append(escapeName(columnTypeId));
        for (String column : columns) {
            sb.append(",").append(escapeName(column));
        }

        sb.append(" FROM ").append(escapeName(tableName)).append(" WHERE");
        if (injectWhere != null) {
            injectWhere.accept(sb);
        }
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
            for (Long id : linkIds) {
                stmt.setLong(offset++, id);
            }
            for (Key key : keys) {
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

    /**
     * Actually stores data into database.
     *
     * @param mapper            Entity data mapper.
     * @param connection        Database connection.
     * @param tableName         Database table name.
     * @param linkId            Link identifier.
     * @param values0           Values to store.
     * @param additionalColumns Additional columns to store also.
     * @throws SQLException On database error.
     */
    protected void store0(
            Mapper mapper,
            Connection connection,
            String tableName,
            long linkId,
            Map<? extends KeySpec, List<Object>> values0,
            AdditionalColumn... additionalColumns
    ) throws SQLException {
        // Deleting previous values
        delete(connection, tableName, linkId, values0.keySet());

        // Filtering empty values and checking that are
        Map<? extends KeySpec, List<Object>> values = values0.entrySet().stream()
                .filter($ -> !$.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (values.isEmpty()) {
            return;
        }

        String[] columns = mapper.getColumnNames();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(escapeName(tableName));
        sb.append(" (").append(escapeName(columnLinkId)).append(",").append(escapeName(columnTypeId));
        for (AdditionalColumn additionalColumn : additionalColumns) {
            sb.append(",").append(escapeName(additionalColumn.getColumnName()));
        }
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
                sb.append("(?,?");
                for (int j = 0; j < additionalColumns.length + columns.length; j++) {
                    sb.append(",?");
                }
                sb.append(")");
            }
        }

        int offset = 1;
        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (Map.Entry<? extends KeySpec, List<Object>> entry : values.entrySet()) {
                for (Object value : entry.getValue()) {
                    stmt.setLong(offset++, linkId);
                    stmt.setInt(offset++, entry.getKey().getTypeId());
                    for (AdditionalColumn injector : additionalColumns) {
                        injector.getInjector().accept(stmt, offset++);
                    }
                    mapper.setPlaceholdersValue(stmt, offset, value);
                    offset += columns.length;
                }
            }

            stmt.executeUpdate();
        }
    }

    /**
     * Utility method to be used in delete statements.
     * Appends WHERE condition and executes query.
     *
     * @param sb         String builder with query prefix.
     * @param connection Database connection.
     * @param linkId     Link identifier.
     * @param keys       Keys to delete.
     * @throws SQLException On database error.
     */
    protected void appendWhereAndExecute(
            StringBuilder sb,
            Connection connection,
            long linkId,
            Collection<? extends KeySpec> keys
    ) throws SQLException {
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

    @FunctionalInterface
    public interface WhereClauseInjector {
        void accept(StringBuilder sb) throws SQLException;
    }

    @FunctionalInterface
    public interface PlaceholderInjector {
        void accept(PreparedStatement stmt, int offset) throws SQLException;
    }

    /**
     * Defines additional column layout can write for its own needs.
     */
    public static final class AdditionalColumn {
        private final String columnName;
        private final PlaceholderInjector injector;

        public AdditionalColumn(String columnName, PlaceholderInjector injector) {
            this.columnName = Objects.requireNonNull(columnName, "columnName");
            this.injector = Objects.requireNonNull(injector, "injector");
        }

        public String getColumnName() {
            return columnName;
        }

        public PlaceholderInjector getInjector() {
            return injector;
        }
    }
}
