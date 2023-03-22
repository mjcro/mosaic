package io.github.mjcro.mosaic.exceptions;

/**
 * Exception thrown when no suitable type handler foung.
 */
public class NoSuitableTypeHandlerFoundException extends MosaicException {
    public NoSuitableTypeHandlerFoundException(final Class<?> clazz) {
        super(
                clazz == null
                        ? "Unable to resolve type handler for null"
                        : "Unable to resolve type handler for " + clazz.getName()
        );
    }
}
