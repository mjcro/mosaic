package org.github.mjcro.mosaic.example.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public class Amount {
    private final Currency currency;
    private final BigDecimal value;

    public Amount(final Currency currency, final BigDecimal value) {
        this.currency = Objects.requireNonNull(currency, "currency");
        this.value = Objects.requireNonNull(value, "value");
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getValue() {
        return value;
    }
}
