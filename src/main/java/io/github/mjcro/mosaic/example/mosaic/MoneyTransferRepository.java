package io.github.mjcro.mosaic.example.mosaic;

import io.github.mjcro.mosaic.AbstractMappedRepository;
import io.github.mjcro.mosaic.DataProvider;
import io.github.mjcro.mosaic.example.domain.MoneyTransfer;

import java.sql.SQLException;
import java.util.Optional;

public class MoneyTransferRepository extends AbstractMappedRepository<MoneyTransfer, MoneyTransferSpec> {
    public MoneyTransferRepository(final DataProvider<MoneyTransferSpec> dataProvider) {
        super(dataProvider, "moneyTransferData", new MoneyTransferEntityMapper());
    }

    @Override
    public Optional<MoneyTransfer> findById(long id) throws SQLException {
        return super.findById(id);
    }
}
