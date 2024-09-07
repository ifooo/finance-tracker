package com.parcels.infrastucture.extended;

import com.parcels.Application;
import com.parcels.infrastucture.db.PostgresContainerInitializer;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.BootstrapWith;
import org.springframework.transaction.annotation.Transactional;

@BootstrapWith(SpringBootTestContextBootstrapper.class)
@SpringBootTest(classes = {Application.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class BaseDbTest extends PostgresContainerInitializer {
}
