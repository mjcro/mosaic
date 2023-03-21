package io.github.mjcro.mosaic.handlers.sql.mappers;

import io.github.mjcro.mosaic.handlers.sql.Mapper;

public abstract class AbstractSingleColumnValueMapper implements Mapper {
    private static final String[] columns = new String[]{"`value`"};

    @Override
    public String[] getColumnNames() {
        return columns;
    }
}
