package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.exceptions.NoSuitableTypeHandlerFoundException;
import io.github.mjcro.mosaic.handlers.sql.Layout;
import io.github.mjcro.mosaic.handlers.sql.LayoutAwareTypeHandler;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.util.HashMap;
import java.util.Objects;

/**
 * Simple mutable type handler resolver working on top of hash map.
 * Not thread safe (works as builder) and should be configured before actual usage.
 */
public class TypeHandlerResolverMap implements TypeHandlerResolver {
    private final HashMap<Class<?>, TypeHandler> map = new HashMap<>();

    /**
     * Modifies current resolver adding new type handler.
     *
     * @param clazz       Class to add type handler for.
     * @param typeHandler Type handler.
     * @return Self.
     */
    public TypeHandlerResolverMap with(final Class<?> clazz, final TypeHandler typeHandler) {
        map.put(
                Objects.requireNonNull(clazz, "clazz"),
                Objects.requireNonNull(typeHandler, "typeHandler")
        );
        return this;
    }

    /**
     * Modifies current resolver adding new type handler.
     *
     * @param clazz  Class to add type handler for.
     * @param layout SQL layout to use.
     * @param mapper SQL data mapper to use.
     * @return Self.
     */
    public TypeHandlerResolverMap with(Class<?> clazz, Layout layout, Mapper mapper) {
        return this.with(clazz, new LayoutAwareTypeHandler(layout, mapper));
    }

    @Override
    public TypeHandler resolve(final Class<?> clazz) throws NoSuitableTypeHandlerFoundException {
        if (clazz != null) {
            TypeHandler typeHandler = map.get(clazz);
            if (typeHandler != null) {
                return typeHandler;
            }
        }
        throw new NoSuitableTypeHandlerFoundException(clazz);
    }
}
