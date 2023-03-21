package io.github.mjcro.mosaic.exceptions;

import java.sql.SQLException;

public abstract class MosaicException extends SQLException {
    protected MosaicException(final String message) {
        super(message);
    }
}
