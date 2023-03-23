package io.github.mjcro.mosaic.exceptions;

import java.lang.reflect.Type;

/**
 * Exception thrown when no suitable type handler foung.
 */
public class NoSuitableTypeHandlerFoundException extends MosaicException {
    public NoSuitableTypeHandlerFoundException(final Type type) {
        super(
                type == null
                        ? "Unable to resolve type handler for null"
                        : "Unable to resolve type handler for " + type.getTypeName()
        );
    }
}
