package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.KeySpec;
import io.github.mjcro.mosaic.TypeHandler;
import io.github.mjcro.mosaic.handlers.sql.LayoutAwareTypeHandler;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.util.EnumMapBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StringMapperTest {
    private Connection connection;
    private TypeHandler handler;

    @BeforeMethod
    public void setup() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'src/test/resources/string.sql'");
        handler = new LayoutAwareTypeHandler(MySqlMinimalLayout.INSTANCE, new StringMapper());
    }

    @AfterMethod
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
        Assert.assertTrue(found.isEmpty());

        found = handler.findByLinkId(connection, "common", 43, Collections.singleton(Key.LAST_NAME));
        Assert.assertFalse(found.isEmpty());
        Assert.assertEquals(found.size(), 1);
        Assert.assertEquals(found.get(Key.LAST_NAME).get(0), "Williams");
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
        Assert.assertTrue(found.isEmpty());
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