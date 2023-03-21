package org.github.mjcro.mosaic;

import java.sql.SQLException;

public class UnableToResolveTypeHandlerException extends SQLException {
    public UnableToResolveTypeHandlerException(final Class<?> clazz) {
        super(
                clazz == null
                        ? "Unable to resolve type handler for null"
                        : "Unable to resolve type handler for " + clazz.getName()
        );
    }
}
