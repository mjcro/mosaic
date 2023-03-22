package io.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface
public interface ConnectionProvider {
    /**
     * Wraps given Java supplier into ConnectionProvider wrapper.
     *
     * @param supplier Java supplier.
     * @return ConnectionProvider instance.
     */
    static ConnectionProvider ofSupplier(final Supplier<Connection> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier::get;
    }

    /**
     * Wraps existing connection into ConnectionProvider wrapper.
     *
     * @param connection Established connection
     * @return ConnectionProvider instance.
     */
    static ConnectionProvider ofConnection(final Connection connection) {
        Objects.requireNonNull(connection, "connection");
        return () -> connection;
    }

    /**
     * Constructs or pools database connection.
     *
     * @return Connection to use.
     * @throws SQLException On connection error.
     */
    Connection getConnection() throws SQLException;
}
