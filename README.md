# Mosaic

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mjcro/mosaic.svg)](https://central.sonatype.com/artifact/io.github.mjcro/mosaic)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-007396.svg)](https://adoptium.net/)
[![CI](https://github.com/mjcro/mosaic/actions/workflows/ci.yml/badge.svg)](https://github.com/mjcro/mosaic/actions/workflows/ci.yml)

A small Java framework for storing **sparse entity data in vertical (EAV-style) tables** — one table per Java data type, instead of one wide table per entity.

## Why

Sparse entities lead to wide tables full of `NULL`s. Mosaic flips this: an entity is decomposed into typed key/value fragments, and each fragment is stored in a table dedicated to its Java type (`String` values in one table, `Long` in another, `Instant` in a third, custom types wherever you decide). Rows are linked back to the entity by a numeric `linkId`.

Mosaic is **not** an ORM and does not generate or migrate schema — it only reads, writes and deletes.

## Concept

```
                    ┌───────────────────────┐
                    │   userString          │  ← FIRST_NAME, LAST_NAME
   entity id ────►  ├───────────────────────┤
   (linkId)         │   userLong            │  ← PRICING_PLAN
                    ├───────────────────────┤
                    │   userInstant         │  ← CREATED_AT, EXPIRY_AT
                    └───────────────────────┘
```

Three concepts cover the whole API, each with a single responsibility:

| Concept      | Responsibility                                                                                                |
| ------------ | ------------------------------------------------------------------------------------------------------------- |
| `Repository` | **Orchestrates data** — groups values by Java class, picks the right type handler and routes calls to it.    |
| `Layout`     | **Defines row shape and lifecycle** — which columns make up a row, how rows are inserted, updated, deleted.   |
| `Mapper`     | **Defines value (de)serialization** — how a single Java value reads from `ResultSet` and writes to `PreparedStatement`, and which columns it occupies. |

A `TypeHandlerResolver` ties a Java class to a `(Layout, Mapper)` pair. The `Repository` then routes each value to the table chosen by its mapper, using the storage strategy chosen by its layout.

### Complex types

A mapper is not limited to a single column. Composite values are supported by writing a multi-column `Mapper` — no changes to the layout or repository needed. For example, an `Amount(Currency, BigDecimal)` can be stored as two columns `currencyCode` + `amount` simply by declaring them in the mapper:

```java
public class AmountMapper implements Mapper {
    private static final String[] COLUMNS = {"`currencyCode`", "`amount`"};

    @Override public String getCommonName()    { return "Amount"; }
    @Override public String[] getColumnNames() { return COLUMNS; }

    @Override
    public void setPlaceholdersValue(PreparedStatement stmt, int offset, Object value) throws SQLException {
        Amount a = (Amount) value;
        stmt.setString(offset,     a.getCurrency().getCurrencyCode());
        stmt.setBigDecimal(offset + 1, a.getValue());
    }

    @Override
    public Object readObjectValue(ResultSet rs, int offset) throws SQLException {
        return new Amount(Currency.getInstance(rs.getString(offset)), rs.getBigDecimal(offset + 1));
    }
}
```

Register it like any other mapper and `Amount` becomes a first-class type — stored, queried and deleted through the same `Repository` API.

## Installation

```xml
<dependency>
    <groupId>io.github.mjcro</groupId>
    <artifactId>mosaic</artifactId>
    <version>1.2.0</version>
</dependency>
```

Requires **Java 8+**. Tested on JDK 8, 11, 21 and 25.

## Quick start

**1. Describe entity fields as a `KeySpec` enum.**

```java
public enum UserKey implements KeySpec {
    FIRST_NAME(1, String.class),
    LAST_NAME(2, String.class),
    PRICING_PLAN(5, Long.class),
    CREATED_AT(11, Instant.class);

    private final int typeId;
    private final Class<?> dataClass;

    UserKey(int typeId, Class<?> dataClass) {
        this.typeId = typeId;
        this.dataClass = dataClass;
    }

    @Override public int getTypeId() { return typeId; }
    @Override public Class<?> getDataClass() { return dataClass; }
}
```

**2. Configure type handlers.**

```java
TypeHandlerResolver resolver = new TypeHandlerResolverMap()
        .with(String.class,  MySqlMinimalLayout.DEFAULT, new StringMapper())
        .with(Long.class,    MySqlMinimalLayout.DEFAULT, new LongMapper())
        .with(Instant.class, MySqlMinimalLayout.DEFAULT, new InstantSecondsMapper());
```

**3. Build a repository.**

```java
Repository<UserKey> users = new Repository<>(
        connectionProvider,        // anything yielding java.sql.Connection
        resolver,
        UserKey.class,
        "user"                     // table prefix → userString, userLong, userInstant, ...
);
```

**4. Read, write and delete.**

```java
Map<UserKey, List<Object>> data = EnumMapBuilder.ofClass(UserKey.class)
        .putSingle(UserKey.FIRST_NAME, "John")
        .putSingle(UserKey.LAST_NAME,  "Smith")
        .putSingle(UserKey.PRICING_PLAN, 42L)
        .putSingle(UserKey.CREATED_AT, Instant.now())
        .build();

users.store(8L, data);

Map<UserKey, List<Object>> read = users.findById(8L);
Map<UserKey, List<Object>> some = users.findById(8L, List.of(UserKey.FIRST_NAME));

users.delete(8L, List.of(UserKey.LAST_NAME));   // partial
users.delete(8L);                                // full
```

## Repositories

Different execution strategies are exposed as separate repository classes:

| Class                                      | Purpose                                                                     |
| ------------------------------------------ | --------------------------------------------------------------------------- |
| `Repository`                               | Default. Acquires a connection per operation from a `ConnectionProvider`.   |
| `TransactionalRepository`                  | Caller supplies the `Connection`, so calls join an external transaction.    |
| `ParallelRepository`                       | Fans per-class queries out to an `ExecutorService`.                         |
| `DistributedWriteLockingRepositoryDecorator` | Composition decorator that wraps any of the above in a per-entity distributed lock around `store` / `delete`. Reads pass through unlocked. |

## Built-in SQL layouts

| Layout                                                          | Behavior                                                                                   |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `MySqlMinimalLayout`                                            | `id, linkId, typeId, value…`. Hard delete.                                                 |
| `MySqlPersistentWithCreationTimeSeconds`                        | Adds `active` flag and creation `time`. Soft delete.                                       |
| `MySqlPersistentWithChangesAndCreationModificationTimeSeconds`  | Adds `created`/`modified` timestamps and skips writes when the value is unchanged.         |

Layouts can detect transactional context and append `FOR UPDATE` to reads.

## Built-in mappers

`StringMapper`, `LongMapper`, `InstantSecondsMapper`, `InstantMillisMapper`, `BigDecimalMapper`. Each defines a table suffix (e.g. `String`, `Long`, `Instant`) appended to the repository prefix. Override the suffix via `mapper.withCommonName("Discount")` to split a single Java type across multiple tables. For custom types, implement `Mapper` directly — single- or multi-column.

## Limitations

- Not an ORM — no schema generation or migrations.
- Each Java class maps to exactly one `(Layout, Mapper)` pair per repository; splitting a class across multiple tables requires distinct mappers with different common names.
