package org.github.mjcro.mosaic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface
public interface ConnectionSupplier {
    /**
     * Wraps given Java supplier into ConnectionSupplier wrapper.
     *
     * @param supplier Java supplier.
     * @return ConnectionSupplier instance.
     */
    static ConnectionSupplier ofSupplier(final Supplier<Connection> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier::get;
    }

    /**
     * Constructs or pools database connection.
     *
     * @return Connection to use.
     * @throws SQLException On connection error.
     */
    Connection getConnection() throws SQLException;
}
