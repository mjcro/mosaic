package org.github.mjcro.mosaic;

public interface KeySpec {
    String getName();

    Class<?> getDataClass();

    default boolean isAllowedMultiple() {
        return false;
    }
}
