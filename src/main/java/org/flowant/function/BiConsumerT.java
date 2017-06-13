package org.flowant.function;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface BiConsumerT<T, U> extends BiConsumer<T, U> {
    default void accept(T t, U u) {
        try {
            throwableAccept(t, u);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    void throwableAccept(T t, U u) throws Throwable;
}
