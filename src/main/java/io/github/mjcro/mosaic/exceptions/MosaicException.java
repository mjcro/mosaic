package io.github.mjcro.mosaic.exceptions;

import java.sql.SQLException;

/**
 * Base SQL exception for Mosaic.
 */
public abstract class MosaicException extends SQLException {
    protected MosaicException(final String message) {
        super(message);
    }
}
