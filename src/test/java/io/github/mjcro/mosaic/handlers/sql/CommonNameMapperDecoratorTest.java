package io.github.mjcro.mosaic.handlers.sql;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;

public class CommonNameMapperDecoratorTest {
    @Test
    public void testWithCommonName() {
        Mapper mapper = new CustomMapper();
        Assertions.assertEquals("CustomColumn", mapper.getCommonName());

        // First decoration
        Mapper decorated = mapper.withCommonName("NewCommonName");
        Assertions.assertEquals("NewCommonName", decorated.getCommonName());
        Assertions.assertTrue(decorated instanceof CommonNameMapperDecorator);

        CommonNameMapperDecorator commonNameMapperDecorator = (CommonNameMapperDecorator) decorated;
        Assertions.assertSame(mapper, commonNameMapperDecorator.getDecorated());

        // Second decoration
        decorated = decorated.withCommonName("SecondDecoration");
        Assertions.assertEquals("SecondDecoration", decorated.getCommonName());
        Assertions.assertTrue(decorated instanceof CommonNameMapperDecorator);

        commonNameMapperDecorator = (CommonNameMapperDecorator) decorated;
        Assertions.assertSame(mapper, commonNameMapperDecorator.getDecorated());
    }

    private static class CustomMapper implements Mapper {
        @Override
        public String getCommonName() {
            return "CustomColumn";
        }

        @Override
        public String[] getColumnNames() {
            return new String[0];
        }

        @Override
        public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {

        }

        @Override
        public Object readObjectValue(ResultSet resultSet, int offset) throws SQLException {
            return null;
        }
    }
}
