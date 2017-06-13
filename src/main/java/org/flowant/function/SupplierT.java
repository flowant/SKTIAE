package org.flowant.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierT<T> extends Supplier<T> {
    default T get() {
        try {
            return throwableGet();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    T throwableGet() throws Throwable;
}