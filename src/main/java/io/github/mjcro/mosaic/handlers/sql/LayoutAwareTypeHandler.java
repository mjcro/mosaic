package io.github.mjcro.mosaic.handlers.sql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.TypeHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LayoutAwareTypeHandler implements TypeHandler {
    private final Layout layout;
    private final Mapper mapper;
    private final String commonTableName;

    public LayoutAwareTypeHandler(Layout layout, Mapper mapper, String commonTableName) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.commonTableName = Objects.requireNonNull(commonTableName, "commonTableName");
    }

    public String getTableName(String tablePrefix) {
        return Objects.requireNonNull(tablePrefix, "prefix") + commonTableName;
    }

    @Override
    public <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(
            Connection connection,
            String tablePrefix,
            Collection<Long> linkIds,
            Collection<Key> keys
    ) throws SQLException {
        return layout.findByLinkId(mapper, connection, getTableName(tablePrefix), linkIds, keys);
    }

    @Override
    public void store(
            Connection connection,
            String tablePrefix,
            long linkId,
            Map<? extends KeySpec, List<Object>> values
    ) throws SQLException {
        layout.store(mapper, connection, getTableName(tablePrefix), linkId, values);
    }

    @Override
    public <Key extends KeySpec> void delete(
            Connection connection,
            String tablePrefix,
            long linkId,
            Collection<Key> keys
    ) throws SQLException {
        layout.delete(connection, getTableName(tablePrefix), linkId, keys);
    }
}
