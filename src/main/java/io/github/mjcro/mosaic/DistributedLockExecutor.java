package io.github.mjcro.mosaic;

import java.sql.SQLException;

/**
 * Executes a piece of code while holding a distributed lock identified by a numeric key.
 * <p>
 * Used to serialize concurrent modifications of data belonging to the same entity
 * across multiple application instances or threads.
 */
public interface DistributedLockExecutor {
    /**
     * Acquires distributed lock for given identifier, runs the executable and releases the lock.
     * <p>
     * If the lock cannot be acquired implementation should throw {@link DistributedLockAcquisitionException}.
     *
     * @param id Identifier the lock is bound to. Typically matches the entity identifier being modified.
     * @param e  Executable to run while the lock is held. Not nullable.
     * @throws SQLException On database error or lock acquisition failure.
     */
    void executeLocked(long id, Executable e) throws SQLException;

    /**
     * Unit of work to be executed inside a distributed lock scope.
     */
    interface Executable {
        /**
         * Performs the work.
         *
         * @throws SQLException On database error.
         */
        void execute() throws SQLException;
    }

    /**
     * Thrown when a distributed lock cannot be acquired (e.g. timeout, backend unavailable).
     */
    class DistributedLockAcquisitionException extends SQLException {
        /**
         * Constructs new exception with given message.
         *
         * @param message Human readable error description. Nullable.
         */
        public DistributedLockAcquisitionException(String message) {
            super(message);
        }
    }
}
