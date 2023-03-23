package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.exceptions.NoSuitableTypeHandlerFoundException;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeHandlerResolver {
    /**
     * Returns type handler to use for data read/write.
     *
     * @param type Java type to resolve.
     * @return Associated type handler.
     * @throws NoSuitableTypeHandlerFoundException If no type handler for particular type configured.
     */
    TypeHandler resolve(Type type) throws NoSuitableTypeHandlerFoundException;
}
