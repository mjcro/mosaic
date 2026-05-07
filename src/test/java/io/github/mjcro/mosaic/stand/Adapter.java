package io.github.mjcro.mosaic.stand;

import io.github.mjcro.interfaces.sql.ConnectionProvider;
import io.github.mjcro.mosaic.AbstractConnectionProviderRepository;
import io.github.mjcro.mosaic.TransactionalRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Unifies different repository APIs behind a single store/delete contract for benchmarking.
 */
public interface Adapter {
    /**
     * Wraps a connection-provider-backed repository.
     *
     * @param r Repository to wrap, not null.
     * @return Adapter delegating directly to the repository, not null.
     */
    static Adapter ofAbstractConnectionProviderRepository(AbstractConnectionProviderRepository<Keys> r) {
        return new Adapter() {
            @Override
            public void store(final long id, final Map<Keys, List<Object>> values) throws SQLException {
                r.store(id, values);
            }

            @Override
            public void delete(final long id) throws SQLException {
                r.delete(id);
            }
        };
    }

    /**
     * Wraps a transactional repository, opening a connection and committing per call.
     *
     * @param connectionProvider Source of JDBC connections, not null.
     * @param r                  Repository to wrap, not null.
     * @return Adapter performing each operation in its own transaction, not null.
     */
    static Adapter ofTransactionalRepository(ConnectionProvider connectionProvider, TransactionalRepository<Keys> r) {
        return new Adapter() {
            @Override
            public void store(final long id, final Map<Keys, List<Object>> values) throws SQLException {
                try (Connection conn = connectionProvider.getConnection()) {
                    conn.setAutoCommit(false);
                    r.store(conn, id, values);
                    conn.commit();
                }
            }

            @Override
            public void delete(final long id) throws SQLException {
                try (Connection conn = connectionProvider.getConnection()) {
                    conn.setAutoCommit(false);
                    r.delete(conn, id);
                    conn.commit();
                }
            }
        };
    }

    /**
     * Stores values for the given identifier.
     *
     * @param id     Entity identifier.
     * @param values Values to persist, not null.
     * @throws SQLException On storage failure.
     */
    void store(long id, Map<Keys, List<Object>> values) throws SQLException;

    /**
     * Deletes all data for the given identifier.
     *
     * @param id Entity identifier.
     * @throws SQLException On deletion failure.
     */
    void delete(long id) throws SQLException;
}
