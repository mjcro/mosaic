package org.github.mjcro.mosaic;

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

    @Override
    public TypeHandler resolve(final Class<?> clazz) throws UnableToResolveTypeHandlerException {
        if (clazz != null) {
            TypeHandler typeHandler = map.get(clazz);
            if (typeHandler != null) {
                return typeHandler;
            }
        }
        throw new UnableToResolveTypeHandlerException(clazz);
    }
}
