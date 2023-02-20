package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface TypeHandler {
    void create(Connection connection, String tablePrefix, Map<KeySpec, List<Object>> values);
}
