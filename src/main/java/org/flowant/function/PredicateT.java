package org.flowant.function;

import java.util.function.Predicate;

@FunctionalInterface
public interface PredicateT<T> extends Predicate<T> {
    default boolean test(T t) {
        try {
            return throwableTest(t);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    boolean throwableTest(T t) throws Throwable;
}
