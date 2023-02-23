package org.github.mjcro.mosaic.example.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class MoneyTransferImpl implements MoneyTransfer {
    private final long id, senderId, recipientId;
    private final Amount send, receive, fee, buy;
    private final String description, beneficiary;
    private final Instant created, scheduled, processed;

    public MoneyTransferImpl(
            final long id,
            final long senderId,
            final long recipientId,
            final Amount send,
            final Amount receive,
            final Amount fee,
            final Amount buy,
            final String description,
            final String beneficiary,
            final Instant created,
            final Instant scheduled,
            final Instant processed
    ) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.send = Objects.requireNonNull(send, "send");
        this.receive = Objects.requireNonNull(receive, "receive");
        this.fee = fee;
        this.buy = buy;
        this.description = description;
        this.beneficiary = beneficiary;
        this.created = Objects.requireNonNull(created, "created");
        this.scheduled = scheduled;
        this.processed = processed;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getSenderId() {
        return senderId;
    }

    @Override
    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public Amount getAmountToSend() {
        return send;
    }

    @Override
    public Amount getAmountToReceive() {
        return receive;
    }

    @Override
    public Optional<Amount> getFeeAmount() {
        return Optional.ofNullable(fee);
    }

    @Override
    public Optional<Amount> getBuyRate() {
        return Optional.ofNullable(buy);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> getBeneficiaryName() {
        return Optional.ofNullable(beneficiary);
    }

    @Override
    public Instant getCreatedAt() {
        return created;
    }

    @Override
    public Optional<Instant> getScheduledAt() {
        return Optional.ofNullable(scheduled);
    }

    @Override
    public Optional<Instant> getProcessedAt() {
        return Optional.ofNullable(processed);
    }
}
