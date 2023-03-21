package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TypeHandler {
    void create(Connection connection, String tablePrefix, long id, Map<? extends KeySpec, List<Object>> values) throws SQLException;

    void update(Connection connection, String tablePrefix, long id, Map<KeySpec, List<Object>> values) throws SQLException;

    <Key extends KeySpec> void delete(Connection connection, String tablePrefix, long id, Collection<Key> keys) throws SQLException;

    <Key extends KeySpec> Map<Long, Map<Key, List<Object>>> findById(
            Connection connection,
            String tablePrefix,
            Collection<Long> ids,
            Collection<Key> keys
    ) throws SQLException;
}
