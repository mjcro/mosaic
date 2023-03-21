package io.github.mjcro.mosaic.example;

import io.github.mjcro.mosaic.ConnectionProvider;
import io.github.mjcro.mosaic.DataProvider;
import io.github.mjcro.mosaic.TypeHandlerResolver;
import io.github.mjcro.mosaic.TypeHandlerResolverMap;
import io.github.mjcro.mosaic.example.domain.Amount;
import io.github.mjcro.mosaic.example.mosaic.AmountMapper;
import io.github.mjcro.mosaic.example.mosaic.MoneyTransferRepository;
import io.github.mjcro.mosaic.example.mosaic.MoneyTransferSpec;
import io.github.mjcro.mosaic.handlers.sql.mappers.InstantSecondsMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.LongMapper;
import io.github.mjcro.mosaic.handlers.sql.mappers.StringMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Configuring connection provider
        if (args.length != 1) {
            throw new IllegalArgumentException("Exactly one argument with database DSN expected");
        }
        String dsn = args[0];
        ConnectionProvider connectionProvider = () -> DriverManager.getConnection(dsn);

        // Configuring type handlers
        TypeHandlerResolver typeHandlerResolver = new TypeHandlerResolverMap()
                .with(Long.class, MySqlMinimalLayout.INSTANCE, new LongMapper())
                .with(String.class, MySqlMinimalLayout.INSTANCE, new StringMapper())
                .with(Instant.class, MySqlMinimalLayout.INSTANCE, new InstantSecondsMapper())
                .with(Amount.class, MySqlMinimalLayout.INSTANCE, new AmountMapper());

        // Configuring data provider
        DataProvider<MoneyTransferSpec> dataProvider = new DataProvider<>(connectionProvider, typeHandlerResolver);
        dataProvider.test();
        System.out.println("Connection established");

        // Creating repository
        MoneyTransferRepository repository = new MoneyTransferRepository(dataProvider);

        System.out.println(repository.findById(1));
    }
}
