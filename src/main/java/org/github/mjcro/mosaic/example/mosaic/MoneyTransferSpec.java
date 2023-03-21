package org.github.mjcro.mosaic.example.mosaic;

import org.github.mjcro.mosaic.KeySpec;
import org.github.mjcro.mosaic.example.domain.Amount;

import java.time.Instant;

public enum MoneyTransferSpec implements KeySpec {
    SENDER_ID(101, Long.class),
    RECIPIENT_ID(102, Long.class),

    AMOUNT_SEND(201, Amount.class),
    AMOUNT_RECEIVE(202, Amount.class),
    AMOUNT_FEE(203, Amount.class),
    AMOUNT_BUY_RATE(204, Amount.class),

    DESCRIPTION(301, String.class),
    BENEFICIARY_NAME(302, String.class),

    CREATED_AT(401, Instant.class),
    SCHEDULED_AT(402, Instant.class),
    PROCESSED_AT(403, Instant.class)
    ;

    private final int typeId;
    private final Class<?> dataClass;

    MoneyTransferSpec(final int typeId, final Class<?> clazz) {
        this.typeId = typeId;
        this.dataClass = clazz;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public Class<?> getDataClass() {
        return dataClass;
    }
}
