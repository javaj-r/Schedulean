package org.javid.schedulean.application.port.out;

public interface SchemaMigrationPort {
    void migrate();

    int currentVersion();

    String databaseType();
}
