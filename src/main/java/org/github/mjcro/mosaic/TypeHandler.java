package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TypeHandler {
    void create(Connection connection, String tablePrefix, long id, Map<KeySpec, List<Object>> values);

    void update(Connection connection, String tablePrefix, long id, Map<KeySpec, List<Object>> values);

    <Key extends Enum<Key> & KeySpec> Map<Long, Map<Key, List<Object>>> findById(
            Connection connection,
            String tablePrefix,
            Collection<Long> ids,
            Collection<Key> keys
    );
}
