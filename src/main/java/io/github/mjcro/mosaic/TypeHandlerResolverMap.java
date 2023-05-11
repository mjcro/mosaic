package io.github.mjcro.mosaic;

import io.github.mjcro.mosaic.exceptions.NoSuitableTypeHandlerFoundException;
import io.github.mjcro.mosaic.handlers.sql.Layout;
import io.github.mjcro.mosaic.handlers.sql.LayoutAwareTypeHandler;
import io.github.mjcro.mosaic.handlers.sql.Mapper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;

/**
 * Simple mutable type handler resolver working on top of hash map.
 * Not thread safe (works as builder) and should be configured before actual usage.
 */
public class TypeHandlerResolverMap implements TypeHandlerResolver {
    private final HashMap<Type, TypeHandler> map = new HashMap<>();

    /**
     * Modifies current resolver adding new type handler.
     *
     * @param clazz       Class to add type handler for.
     * @param typeHandler Type handler.
     * @return Self.
     */
    public TypeHandlerResolverMap with(Class<?> clazz, TypeHandler typeHandler) {
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
    public TypeHandler resolve(Type type) throws NoSuitableTypeHandlerFoundException {
        if (type != null) {
            TypeHandler typeHandler = map.get(type);
            if (typeHandler != null) {
                return typeHandler;
            }
        }
        throw new NoSuitableTypeHandlerFoundException(type);
    }
}
