package io.github.mjcro.mosaic;

/**
 * Key specification.
 * Defines integer identifier used to store data to database and Java type
 * to convert data from/to.
 */
public interface KeySpec {
    /**
     * @return Type identifier to use while storing data into database.
     */
    int getTypeId();

    /**
     * @return Data class.
     */
    Class<?> getDataClass();
}
