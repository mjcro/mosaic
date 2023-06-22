package io.github.mjcro.mosaic.handlers.sql.mysql;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MySqlPersistentWithCreationTimeSecondsTest {
    @Test
    public void testStoreEmptyValues() throws SQLException {
        // Creating schema
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:mosaic_pers_store_1;INIT=RUNSCRIPT FROM 'src/test/resources/repositoryTest.sql'");

        MySqlPersistentWithCreationTimeSeconds layout = new MySqlPersistentWithCreationTimeSeconds(false, "linkId", "typeId", "active", "time");

        HashMap<Key, List<Object>> values = new HashMap<>();
        values.put(Key.FOO, new ArrayList<>()); // Empty list

        layout.store(new StringMapper(), connection, "unitTestStringPersistent", 10, values);
        connection.close();
    }

    public enum Key implements KeySpec {
        FOO(1, String.class),
        BAR(2, String.class);

        private final int typeId;
        private final Class<?> dataClass;

        Key(final int typeId, final Class<?> clazz) {
            this.typeId = typeId;
            this.dataClass = clazz;
        }

        @Override
        public int getTypeId() {
            return typeId;
        }

        @Override
        public Class<?> getDataClass() {
            return dataClass;
        }
    }
}