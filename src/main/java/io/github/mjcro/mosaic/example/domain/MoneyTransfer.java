package io.github.mjcro.mosaic.example.domain;

import java.time.Instant;
import java.util.Optional;

public interface MoneyTransfer {
    long getId();

    long getSenderId();

    long getRecipientId();

    Amount getAmountToSend();

    Amount getAmountToReceive();

    Optional<Amount> getFeeAmount();

    Optional<Amount> getBuyRate();

    Optional<String> getDescription();

    Optional<String> getBeneficiaryName();

    Instant getCreatedAt();

    Optional<Instant> getScheduledAt();

    Optional<Instant> getProcessedAt();
}
