package com.smartprocure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SmartProcureDatabaseIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Flyway migration V1 and schema smartprocure isolation")
    void testFlywayMigrationAndSchemaIsolation() {
        // 1. Verify connection session current_schema is 'smartprocure'
        String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema();", String.class);
        assertEquals("smartprocure", currentSchema, "Session should resolve to 'smartprocure' schema");

        // 2. Verify flyway_schema_history exists in schema smartprocure
        Integer flywayRecords = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM smartprocure.flyway_schema_history WHERE success = true;", Integer.class);
        assertNotNull(flywayRecords);
        assertTrue(flywayRecords >= 1, "At least one Flyway migration must be successfully applied");

        // Verify V1 migration version
        String version = jdbcTemplate.queryForObject(
            "SELECT version FROM smartprocure.flyway_schema_history WHERE installed_rank = 1;", String.class);
        assertEquals("1", version, "First migration must be version 1");

        // 3. Verify all four domain tables exist in smartprocure schema
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'smartprocure' ORDER BY table_name;",
            String.class);

        assertTrue(tables.contains("users"), "Table 'users' must exist in smartprocure");
        assertTrue(tables.contains("purchase_orders"), "Table 'purchase_orders' must exist in smartprocure");
        assertTrue(tables.contains("po_line_items"), "Table 'po_line_items' must exist in smartprocure");
        assertTrue(tables.contains("approval_history"), "Table 'approval_history' must exist in smartprocure");
        assertTrue(tables.contains("flyway_schema_history"), "Table 'flyway_schema_history' must exist in smartprocure");

        // 4. Verify public schema tables remain untouched (exactly 7 tables)
        Integer publicTableCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';", Integer.class);
        assertEquals(7, publicTableCount, "Legacy public schema must retain exactly 7 original tables");
    }
}
