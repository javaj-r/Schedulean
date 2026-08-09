package org.javid.schedulean.application.port.out;

import java.util.function.Supplier;

public interface ResilientDatabasePort {
    <T> T executeWithRetry(Supplier<T> operation);

    void executeWithRetry(Runnable operation);

    String circuitState();
}
