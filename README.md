Mosaic
------

(Draft) Micro framework aiming to provide help to store sparse data into multiple database tables (vertical tables).

## Distribution

```xml
<dependency>
    <groupId>io.github.mjcro</groupId>
    <artifactId>mosaic</artifactId>
    <version>1.0.2</version>
</dependency>
```

## Motivation

Sometimes we need to store more or less sparse data in database. Taking into account database
architecture principles and normalization rules we can create one or more tables to store data.
But sparse data leads to sparse rows - where 50% or even more columns are `NULL`.

We can mitigate this by clustering our knowledge of stored data and creating subset of tables,
each one containing only columns that are typical for some kind of data.

Mosaic offers different approach - split storing entity into chunks and different chunk store
in different table. For example all strings are stored in one table, timestamps in other,
custom type somewhere else and so on.

## Limitations

- Mosaic is not an ORM
- Mosaic does not provide utility to create or migrate data
- Mosaic (at this moment) supports separate tables for different classes - there is no  
  out of the box possibility to store `String` values in two or more tables.

## Quick how-to guide

### Introduction

Central component that does all the magic is `io.github.mjcro.mosaic.Repository` - it can
read, write and delete sparse data.

In order to create it, next data should be passed in constructor:

- `ConnectionProvider` - something able to return `java.sql.Connection`. Data source,
  connection pool - everything fits.
- `TypeHandlerResolver` - something able to return `TypeHandler` for each class. More details below.
- Key class
- Database table name

### Decouple entity

So let's start with simple entity we want to store using Mosaic:

```java
interface User {
    long getId();

    Optional<String> getFirstName();

    Optional<String> getLastName();

    Optional<String> getBillingAddress();

    Optional<String> getShippingAddress();
}
```

To represent this (and any other entity) in Mosaic, enumeration implementing `io.github.mjcro.mosaic.KeySpec`
should be created:

```java
public enum UserKeySpec implements KeySpec {
    FIRST_NAME(1, String.class),
    LAST_NAME(2, String.class),
    BILLING_ADDRESS(50, String.class),
    SHIPPING_ADDRESS(60, String.class);

    private final int typeId;
    private final Class<?> dataClass;

    Key(int typeId, Class<?> clazz) {
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
```

### Configure type handlers

Key specification above uses only strings. In order to work with them, `TypeHandlerResolver` should
be configured:

```java
TypeHandlerResolver typeHandlerResolver=new TypeHandlerResolverMap()
        .with(String.class,MySqlMinimalLayout.INSTANCE,new StringMapper());
```

This will create type handler resolver which can handle strings only and will store data into
MySQL using minimal layout(id,linkId,typeId,value) using `StringMapper`.

### Instantiating repository

```java
Repository<Key> provider=new Repository<>(
        connectionProvider,
        typeHandlerResolver,
        UserKeySpec.class,
        "user" // Table prefix
        );
```

Thats it. Repository is ready to store and read data (using maps and collections).
`StringMapper` mapper used in type resolver will append suffix `String` so resulting
table name will be `userString`.

### What's next?

You may add new fields to `UserKeySpec`, new data types (Mosaic bundles with mappers
for strings, longs and instants - all using MySQL layouts), declare own mappers for
custom data types (see `RepositoryTest`) and use it.