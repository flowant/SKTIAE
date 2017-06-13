package org.flowant.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerT<T> extends Consumer<T> {
    default void accept(T t) {
        try {
            throwableAccept(t);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    void throwableAccept(T t) throws Throwable;
}
