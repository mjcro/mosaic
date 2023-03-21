package org.github.mjcro.mosaic.example.mosaic;

import org.github.mjcro.mosaic.Entity;
import org.github.mjcro.mosaic.EntityMapper;
import org.github.mjcro.mosaic.example.domain.Amount;
import org.github.mjcro.mosaic.example.domain.MoneyTransfer;
import org.github.mjcro.mosaic.example.domain.MoneyTransferImpl;
import org.github.mjcro.mosaic.util.EnumMapBuilder;

import java.time.Instant;

public class MoneyTransferEntityMapper implements EntityMapper<MoneyTransfer, MoneyTransferSpec> {
    @Override
    public Class<MoneyTransferSpec> getKeySpecClass() {
        return MoneyTransferSpec.class;
    }

    @SuppressWarnings({"DataFlowIssue", "OptionalGetWithoutIsPresent"})
    @Override
    public MoneyTransfer fromEntity(final Entity<MoneyTransferSpec> entity) {
        return new MoneyTransferImpl(
                entity.getId().getAsLong(),
                (long) entity.getSingle(MoneyTransferSpec.SENDER_ID).orElse(null),
                (long) entity.getSingle(MoneyTransferSpec.RECIPIENT_ID).orElse(null),
                (Amount) entity.getSingle(MoneyTransferSpec.AMOUNT_SEND).orElse(null),
                (Amount) entity.getSingle(MoneyTransferSpec.AMOUNT_RECEIVE).orElse(null),
                (Amount) entity.getSingle(MoneyTransferSpec.AMOUNT_FEE).orElse(null),
                (Amount) entity.getSingle(MoneyTransferSpec.AMOUNT_BUY_RATE).orElse(null),
                (String) entity.getSingle(MoneyTransferSpec.DESCRIPTION).orElse(null),
                (String) entity.getSingle(MoneyTransferSpec.BENEFICIARY_NAME).orElse(null),
                (Instant) entity.getSingle(MoneyTransferSpec.CREATED_AT).orElse(null),
                (Instant) entity.getSingle(MoneyTransferSpec.SCHEDULED_AT).orElse(null),
                (Instant) entity.getSingle(MoneyTransferSpec.PROCESSED_AT).orElse(null)
        );
    }

    @Override
    public Entity<MoneyTransferSpec> toEntity(final MoneyTransfer data) {
        EnumMapBuilder<MoneyTransferSpec> builder = EnumMapBuilder.ofClass(MoneyTransferSpec.class);
        builder
                .putSingle(MoneyTransferSpec.SENDER_ID, data.getSenderId())
                .putSingle(MoneyTransferSpec.RECIPIENT_ID, data.getRecipientId())
                .putSingle(MoneyTransferSpec.AMOUNT_SEND, data.getAmountToSend())
                .putSingle(MoneyTransferSpec.AMOUNT_RECEIVE, data.getAmountToReceive())
                .putSingleIfPresent(MoneyTransferSpec.AMOUNT_FEE, data.getFeeAmount())
                .putSingleIfPresent(MoneyTransferSpec.AMOUNT_BUY_RATE, data.getBuyRate())
                .putSingleIfPresent(MoneyTransferSpec.DESCRIPTION, data.getDescription())
                .putSingleIfPresent(MoneyTransferSpec.BENEFICIARY_NAME, data.getBeneficiaryName())
                .putSingle(MoneyTransferSpec.CREATED_AT, data.getCreatedAt())
                .putSingleIfPresent(MoneyTransferSpec.SCHEDULED_AT, data.getScheduledAt())
                .putSingleIfPresent(MoneyTransferSpec.PROCESSED_AT, data.getProcessedAt());

        return new Entity<>(data.getId(), builder.build());
    }
}
