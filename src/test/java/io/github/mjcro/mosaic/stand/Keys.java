package io.github.mjcro.mosaic.stand;

import io.github.mjcro.mosaic.KeySpec;

/**
 * Test {@link KeySpec} with eight {@link Long}-typed entries used by the benchmark stand.
 */
public enum Keys implements KeySpec {
    A(1),
    B(2),
    C(3),
    D(4),
    E(5),
    F(6),
    G(7),
    H(8);

    private final int typeId;

    Keys(int typeId) {
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public Class<?> getDataClass() {
        return Long.class;
    }
}
