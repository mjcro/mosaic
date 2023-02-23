package org.github.mjcro.mosaic.example.mosaic;

import org.github.mjcro.mosaic.AbstractMappedRepository;
import org.github.mjcro.mosaic.DataProvider;
import org.github.mjcro.mosaic.example.domain.MoneyTransfer;

public class MoneyTransferRepository extends AbstractMappedRepository<MoneyTransfer, MoneyTransferSpec> {
    protected MoneyTransferRepository(final DataProvider<MoneyTransferSpec> dataProvider) {
        super(dataProvider, "moneyTransferData", new MoneyTransferEntityMapper());
    }
}
