package io.github.mjcro.mosaic.handlers.sql;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommonNameMapperDecoratorTest {
    @Test
    public void testWithCommonName() {
        Mapper mapper = new CustomMapper();
        Assert.assertEquals(mapper.getCommonName(), "CustomColumn");

        // First decoration
        Mapper decorated = mapper.withCommonName("NewCommonName");
        Assert.assertEquals(decorated.getCommonName(), "NewCommonName");
        Assert.assertTrue(decorated instanceof CommonNameMapperDecorator);

        CommonNameMapperDecorator commonNameMapperDecorator = (CommonNameMapperDecorator) decorated;
        Assert.assertSame(commonNameMapperDecorator.getDecorated(), mapper);

        // Second decoration
        decorated = decorated.withCommonName("SecondDecoration");
        Assert.assertEquals(decorated.getCommonName(), "SecondDecoration");
        Assert.assertTrue(decorated instanceof CommonNameMapperDecorator);

        commonNameMapperDecorator = (CommonNameMapperDecorator) decorated;
        Assert.assertSame(commonNameMapperDecorator.getDecorated(), mapper);
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