package org.github.mjcro.mosaic;

public interface KeySpec {
    int getTypeId();

    Class<?> getDataClass();

    default boolean isAllowedMultiple() {
        return false;
    }
}
