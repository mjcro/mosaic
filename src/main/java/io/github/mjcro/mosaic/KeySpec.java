package io.github.mjcro.mosaic;

import io.github.mjcro.interfaces.classes.WithDataClass;
import io.github.mjcro.interfaces.ints.WithTypeId;

/**
 * Key specification.
 * Defines integer identifier used to store data to database and Java type
 * to convert data from/to.
 */
public interface KeySpec extends WithDataClass, WithTypeId {
    /**
     * @return Type identifier to use while storing data into database.
     */
    @Override
    int getTypeId();

    /**
     * @return Data class.
     */
    @Override
    Class<?> getDataClass();
}
