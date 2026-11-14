package com.parcels.infrastucture.db;

import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresTestContainer extends PostgreSQLContainer {

    private static final String IMAGE_VERSION = "postgres:16";
    private static PostgresTestContainer container;

    private PostgresTestContainer() {
        super(IMAGE_VERSION);
    }

    public static PostgresTestContainer getInstance() {
        if (container == null) {
            container = new PostgresTestContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
    }

}
