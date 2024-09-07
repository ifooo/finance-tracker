package com.parcels.infrastucture.db;

import lombok.Getter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@Getter
public class PostgresContainerInitializer {

    @Container
    public static PostgreSQLContainer<PostgresTestContainer> container =
            PostgresTestContainer.getInstance();
}
