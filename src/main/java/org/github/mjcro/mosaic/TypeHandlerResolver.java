package org.github.mjcro.mosaic;

public interface TypeHandlerResolver {
    /**
     * Returns type handler to use for data read/write.
     *
     * @param clazz Class to resolve.
     * @return Associated type handler.
     * @throws UnableToResolveTypeHandlerException If no type handler for particular type configured.
     */
    TypeHandler resolve(Class<?> clazz) throws UnableToResolveTypeHandlerException;
}
