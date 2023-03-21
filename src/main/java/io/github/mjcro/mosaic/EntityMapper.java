package io.github.mjcro.mosaic;

public interface EntityMapper<T, Key extends KeySpec> {
    Class<Key> getKeySpecClass();

    T fromEntity(Entity<Key> entity);

    Entity<Key> toEntity(T data);
}
