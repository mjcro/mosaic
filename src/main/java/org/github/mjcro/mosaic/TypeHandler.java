package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TypeHandler {
    void create(Connection connection, String tablePrefix, Map<KeySpec, List<Object>> values);

    <ID, Key extends Enum<Key> & KeySpec> List<Entity<ID, Key>> findById(
            Connection connection,
            String tablePrefix,
            Collection<ID> ids,
            Collection<Key> keys
    );
}
