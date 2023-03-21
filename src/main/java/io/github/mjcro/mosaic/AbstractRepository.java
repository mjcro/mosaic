package io.github.mjcro.mosaic;

import java.util.Objects;

public abstract class AbstractRepository<Key extends Enum<Key> & KeySpec> {
    protected final DataProvider<Key> dataProvider;
    protected final String tablePrefix;

    protected AbstractRepository(
            final DataProvider<Key> dataProvider,
            final String tablePrefix
    ) {
        this.dataProvider = Objects.requireNonNull(dataProvider, "dataProvider");
        this.tablePrefix = Objects.requireNonNull(tablePrefix, "tablePrefix");
    }
}
