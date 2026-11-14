package com.parcels.infrastucture.db;

import lombok.Getter;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@Getter
public class PostgresContainerInitializer {

    @Container
    public static PostgreSQLContainer container =
            PostgresTestContainer.getInstance();
}
