package io.github.mjcro.mosaic.exceptions;

public class UnableToResolveTypeHandlerException extends MosaicException {
    public UnableToResolveTypeHandlerException(final Class<?> clazz) {
        super(
                clazz == null
                        ? "Unable to resolve type handler for null"
                        : "Unable to resolve type handler for " + clazz.getName()
        );
    }
}
