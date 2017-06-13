package org.flowant.function;

import java.util.function.BiFunction;

@FunctionalInterface
public interface BiFunctionT<T, U, R> extends BiFunction<T, U, R> {
    default R apply(T t, U u) {
        try {
            return throwableApply(t, u);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    R throwableApply(T t, U u) throws Throwable;
}
