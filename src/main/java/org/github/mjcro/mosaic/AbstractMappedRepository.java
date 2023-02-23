package org.github.mjcro.mosaic;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractMappedRepository<T, Key extends Enum<Key> & KeySpec> extends AbstractRepository<Key> {
    protected final EntityMapper<T, Key> entityMapper;

    protected AbstractMappedRepository(
            final DataProvider<Key> dataProvider,
            final String tablePrefix,
            final EntityMapper<T, Key> entityMapper
    ) {
        super(dataProvider, tablePrefix);
        this.entityMapper = Objects.requireNonNull(entityMapper, "entityMapper");
    }

    protected long create(final T domainObject) throws SQLException {
        return dataProvider.create(tablePrefix, entityMapper.toEntity(domainObject).getValues());
    }

    protected Optional<T> findById(final long id) throws SQLException {
        return findById(Collections.singleton(id)).stream().findAny();
    }

    protected List<T> findById(final Set<Long> identifiers) throws SQLException {
        return dataProvider.findById(entityMapper.getKeySpecClass(), tablePrefix, identifiers)
                .stream()
                .map(entityMapper::fromEntity)
                .collect(Collectors.toList());
    }

    protected void update(final long id, final Map<Key, List<Object>> values) throws SQLException {
        dataProvider.update(tablePrefix, id, values);
    }
}
