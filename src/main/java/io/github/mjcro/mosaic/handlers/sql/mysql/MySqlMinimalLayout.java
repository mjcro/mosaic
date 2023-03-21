package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.Layout;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MySqlMinimalLayout implements Layout {
    @Override
    public <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findByLinkId(final Mapper mapper, final Connection connection, final String tablePrefix, final Collection<Long> linkIds, final Collection<Key> keys) throws SQLException {
        return null;
    }

    @Override
    public void store(final Mapper mapper, final Connection connection, final String tablePrefix, final long linkId, final Map<? extends KeySpec, List<Object>> values) throws SQLException {

    }

    @Override
    public void delete(final Connection connection, final String tableName, final long linkId, final Collection<? extends KeySpec> keys) throws SQLException {

    }
}
