package com.hospital.smart_hospital;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TestDatabaseIsolationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify DataSource uses in-memory H2 database and never connects to MySQL or smart_hospital_db")
    void verifyTestDataSourceIsIsolated() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            String databaseProductName = metaData.getDatabaseProductName();

            // 1. Must be H2
            assertThat(databaseProductName).isEqualToIgnoringCase("H2");

            // 2. Must be an in-memory test database URL
            assertThat(url).contains("jdbc:h2:mem:");

            // 3. Must NEVER be MySQL or point to smart_hospital_db
            assertThat(url).doesNotContain("mysql");
            assertThat(url).doesNotContain("smart_hospital_db");
            assertThat(databaseProductName).doesNotContainIgnoringCase("MySQL");
        }
    }
}

