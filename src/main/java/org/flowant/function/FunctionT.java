package org.flowant.function;

import java.util.function.Function;

@FunctionalInterface
public interface FunctionT<T, R> extends Function<T, R> {
    default R apply(T t) {
        try {
            return throwableApply(t);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    R throwableApply(T t) throws Throwable;
}
