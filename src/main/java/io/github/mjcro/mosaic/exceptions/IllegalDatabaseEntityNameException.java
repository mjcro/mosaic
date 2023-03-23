package io.github.mjcro.mosaic.exceptions;

/**
 * Exception thrown when invalid database table/column name is provided.
 */
public class IllegalDatabaseEntityNameException extends MosaicException {
    public IllegalDatabaseEntityNameException(String name) {
        super(
                name == null || name.isEmpty()
                        ? "Empty table/column name"
                        : "Invalid table/column name " + name
        );
    }
}
