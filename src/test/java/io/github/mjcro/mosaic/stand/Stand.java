package io.github.mjcro.mosaic.stand;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mjcro.interfaces.sql.ConnectionProvider;
import io.github.mjcro.mosaic.DistributedLockingRepository;
import io.github.mjcro.mosaic.Repository;
import io.github.mjcro.mosaic.TransactionalRepository;
import io.github.mjcro.mosaic.TypeHandlerResolverMap;
import io.github.mjcro.mosaic.handlers.sql.mappers.LongMapper;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlMinimalLayout;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithChangesAndCreationModificationTimeSeconds;
import io.github.mjcro.mosaic.handlers.sql.mysql.MySqlPersistentWithCreationTimeSeconds;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manual concurrency benchmark stand for repository implementations.
 * Selects a repository variant by hardcoded mode and hammers it from a fixed-size thread pool.
 */
public class Stand {
    /**
     * Entry point. Builds the configured repository and runs the load loop until interrupted.
     *
     * @param args Ignored.
     * @throws Exception On any setup or runtime failure.
     */
    public static void main(String[] args) throws Exception {
        // Configuration
        int mode = 31;
        int concurrency = 50;

        // Connection pool
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3308/test");
        hikariConfig.setUsername("root");
        hikariConfig.setPassword("root");
        hikariConfig.setMaximumPoolSize(concurrency);
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        ConnectionProvider connectionProvider = dataSource::getConnection;

        // Instantiation
        Adapter adapter;
        if (mode == 11) {
            Repository<Keys> repo = new Repository<>(
                    connectionProvider,
                    new TypeHandlerResolverMap()
                            .with(Long.class, new MySqlMinimalLayout(false, "linkId", "typeId"), new LongMapper()),
                    Keys.class,
                    "minimal"
            );
            adapter = Adapter.ofAbstractConnectionProviderRepository(repo);
        } else if (mode == 12) {
            Repository<Keys> repo = new Repository<>(
                    connectionProvider,
                    new TypeHandlerResolverMap()
                            .with(Long.class, new MySqlMinimalLayout(true, "linkId", "typeId"), new LongMapper()),
                    Keys.class,
                    "minimal"
            );
            adapter = Adapter.ofAbstractConnectionProviderRepository(repo);
        } else if (mode == 13) {
            Repository<Keys> repo = new Repository<>(
                    connectionProvider,
                    new TypeHandlerResolverMap()
                            .with(Long.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new LongMapper()),
                    Keys.class,
                    "persistent"
            );
            adapter = Adapter.ofAbstractConnectionProviderRepository(repo);
        } else if (mode == 14) {
            Repository<Keys> repo = new Repository<>(
                    connectionProvider,
                    new TypeHandlerResolverMap()
                            .with(Long.class, MySqlPersistentWithChangesAndCreationModificationTimeSeconds.DEFAULT, new LongMapper()),
                    Keys.class,
                    "changes"
            );
            adapter = Adapter.ofAbstractConnectionProviderRepository(repo);
        } else if (mode == 22) {
            TransactionalRepository<Keys> repo = new TransactionalRepository<>(
                    new TypeHandlerResolverMap()
                            .with(Long.class, new MySqlMinimalLayout(true, "linkId", "typeId"), new LongMapper()),
                    Keys.class,
                    "minimal"
            );
            adapter = Adapter.ofTransactionalRepository(connectionProvider, repo);
        } else if (mode == 23) {
            TransactionalRepository<Keys> repo = new TransactionalRepository<>(
                    new TypeHandlerResolverMap()
                            .with(Long.class, MySqlPersistentWithCreationTimeSeconds.DEFAULT, new LongMapper()),
                    Keys.class,
                    "persistent"
            );
            adapter = Adapter.ofTransactionalRepository(connectionProvider, repo);
        } else if (mode == 24) {
            TransactionalRepository<Keys> repo = new TransactionalRepository<>(
                    new TypeHandlerResolverMap()
                            .with(Long.class, MySqlPersistentWithChangesAndCreationModificationTimeSeconds.DEFAULT, new LongMapper()),
                    Keys.class,
                    "changes"
            );
            adapter = Adapter.ofTransactionalRepository(connectionProvider, repo);
        } else if (mode == 31) {
            ReentrantLock[] locks = new ReentrantLock[2];
            for (int i = 0; i < locks.length; i++) {
                locks[i] = new ReentrantLock();
            }
            DistributedLockingRepository<Keys> repo = new DistributedLockingRepository<>(
                    (id, e) -> {
                        locks[(int) (id % locks.length)].lock();
                        try {
                            e.execute();
                        } finally {
                            locks[(int) (id % locks.length)].unlock();
                        }
                    },
                    connectionProvider,
                    new TypeHandlerResolverMap()
                            .with(Long.class, MySqlPersistentWithChangesAndCreationModificationTimeSeconds.DEFAULT, new LongMapper()),
                    Keys.class,
                    "changes"
            );
            adapter = Adapter.ofAbstractConnectionProviderRepository(repo);
        } else {
            throw new IllegalArgumentException("Unknown mode " + mode);
        }

        runRepository(mode, concurrency, adapter);
    }

    /**
     * Runs the load loop, periodically printing throughput and error counters.
     *
     * @param mode        Mode identifier used in log lines.
     * @param concurrency Worker thread count.
     * @param adapter     Adapter under test, not null.
     * @throws Exception On thread pool failure.
     */
    private static void runRepository(
            int mode,
            int concurrency,
            Adapter adapter
    ) throws Exception {
        ExecutorService srv = Executors.newFixedThreadPool(concurrency);

        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook running - cleaning up...");
            running.set(false);
            srv.shutdown();
            try {
                srv.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        AtomicLong runs = new AtomicLong();
        AtomicLong successes = new AtomicLong();
        AtomicLong errors = new AtomicLong();

        for (int i = 0; i < concurrency; i++) {
            srv.submit(() -> {
                Random random = new Random();
                Instant start = Instant.now();
                while (running.get()) {
                    int id = random.nextInt(1000);
                    try {
                        adapter.store(id, generateValues(random));
                        successes.incrementAndGet();
                    } catch (SQLException e) {
//                        e.printStackTrace();
                        errors.incrementAndGet();
                    }
                    long run = runs.incrementAndGet();
                    if (run % 100 == 0) {
                        Duration elapsed = Duration.between(start, Instant.now());
                        double elapsedMillis = elapsed.toMillis();
                        double rps = run * 1000. / elapsedMillis;
                        System.out.printf("Runs: [%dM%dT] %d [%d/%d] %.1fRPS\n", mode, concurrency, run, errors.get(), successes.get(), rps);
                    }
                    if (run % 25 == 0) {
                        try {
                            adapter.delete(id);
                        } catch (SQLException e) {
//                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        srv.awaitTermination(1, TimeUnit.HOURS);
    }

    /**
     * Builds a randomized payload covering A-D and optionally E-H based on chance.
     *
     * @param random Source of randomness, not null.
     * @return Map of keys to single-element value lists, not null.
     */
    static Map<Keys, List<Object>> generateValues(Random random) {
        int r = random.nextInt(100);
        EnumMap<Keys, List<Object>> map = new EnumMap<>(Keys.class);
        map.put(Keys.A, Collections.singletonList((long) random.nextInt(10)));
        map.put(Keys.B, Collections.singletonList((long) random.nextInt(10)));
        map.put(Keys.C, Collections.singletonList((long) random.nextInt(10)));
        map.put(Keys.D, Collections.singletonList((long) random.nextInt(10)));
        if (r > 50) {
            map.put(Keys.E, Collections.singletonList((long) random.nextInt(10)));
            map.put(Keys.F, Collections.singletonList((long) random.nextInt(10)));
            if (r > 75) {
                map.put(Keys.G, Collections.singletonList((long) random.nextInt(10)));
                map.put(Keys.H, Collections.singletonList((long) random.nextInt(10)));
            }
        }
        return map;
    }
}
