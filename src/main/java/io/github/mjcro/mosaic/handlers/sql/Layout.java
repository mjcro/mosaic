package io.github.mjcro.mosaic.handlers.sql;

import io.github.mjcro.mosaic.KeySpec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Layout {
    <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(
            final Mapper mapper,
            final Connection connection,
            final String tablePrefix,
            final Collection<Long> linkIds,
            final Collection<Key> keys
    ) throws SQLException;

    void store(
            final Mapper mapper,
            final Connection connection,
            final String tablePrefix,
            final long linkId,
            final Map<? extends KeySpec, List<Object>> values
    ) throws SQLException;

    void delete(
            Connection connection,
            String tableName,
            long linkId,
            Collection<? extends KeySpec> keys
    ) throws SQLException;
}
