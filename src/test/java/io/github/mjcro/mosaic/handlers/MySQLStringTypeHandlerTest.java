package io.github.mjcro.mosaic.handlers;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Execution(ExecutionMode.SAME_THREAD)
class MySQLStringTypeHandlerTest {
    private Connection connection;
    private MySQLStringTypeHandler handler;

    @BeforeEach
    public void setup() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'src/test/resources/string.sql'");
        handler = new MySQLStringTypeHandler();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    public void testCreateAndRead() throws SQLException {
        Map<Key, List<Object>> data;
        data = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "Robin")
                .putSingle(Key.LAST_NAME, "Williams")
                .build();

        handler.store(connection, "common", 43, data);

        data = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.LOGIN, "admin")
                .putSingle(Key.FIRST_NAME, "Billie")
                .build();
        handler.store(connection, "common", 2, data);

        Map<Key, List<Object>> found = handler.findByLinkId(connection, "common", 21, Collections.singleton(Key.LAST_NAME));
        Assertions.assertTrue(found.isEmpty());

        found = handler.findByLinkId(connection, "common", 43, Collections.singleton(Key.LAST_NAME));
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertEquals(1, found.size());
        Assertions.assertEquals("Williams", found.get(Key.LAST_NAME).get(0));
    }

    @Test
    public void testDelete() throws SQLException {
        Map<Key, List<Object>> data = EnumMapBuilder.ofClass(Key.class)
                .putSingle(Key.FIRST_NAME, "Robin")
                .putSingle(Key.LAST_NAME, "Williams")
                .build();
        handler.store(connection, "common", 43, data);

        handler.delete(connection, "common", 43, Collections.singletonList(Key.FIRST_NAME));
        Map<Key, List<Object>> found = handler.findByLinkId(connection, "common", 43, Collections.singleton(Key.FIRST_NAME));
        Assertions.assertTrue(found.isEmpty());

    }

    public enum Key implements KeySpec {
        LOGIN(1),
        FIRST_NAME(2),
        LAST_NAME(3);

        private final int typeId;

        Key(int typeId) {
            this.typeId = typeId;
        }

        @Override
        public int getTypeId() {
            return typeId;
        }

        @Override
        public Class<?> getDataClass() {
            return String.class;
        }
    }
}