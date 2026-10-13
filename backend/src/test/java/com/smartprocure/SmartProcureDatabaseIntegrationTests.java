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
    @DisplayName("Verify Flyway migrations V1 through V3 applied and schema smartprocure isolation")
    void testFlywayMigrationsAndSchemaIsolation() {
        // 1. Verify connection session current_schema is 'smartprocure'
        String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema();", String.class);
        assertEquals("smartprocure", currentSchema, "Session should resolve to 'smartprocure' schema");

        // 2. Verify flyway_schema_history has all 3 migrations successfully applied
        Integer flywayRecords = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM smartprocure.flyway_schema_history WHERE success = true;", Integer.class);
        assertNotNull(flywayRecords);
        assertTrue(flywayRecords >= 3, "At least three Flyway migrations (V1, V2, V3) must be successfully applied");

        // Verify V1, V2, and V3 migration versions
        String v1 = jdbcTemplate.queryForObject(
            "SELECT version FROM smartprocure.flyway_schema_history WHERE installed_rank = 1;", String.class);
        assertEquals("1", v1, "First migration must be version 1");

        String v2 = jdbcTemplate.queryForObject(
            "SELECT version FROM smartprocure.flyway_schema_history WHERE installed_rank = 2;", String.class);
        assertEquals("2", v2, "Second migration must be version 2");

        String v3 = jdbcTemplate.queryForObject(
            "SELECT version FROM smartprocure.flyway_schema_history WHERE installed_rank = 3;", String.class);
        assertEquals("3", v3, "Third migration must be version 3");
    }

    @Test
    @DisplayName("Verify all domain tables and relationships exist across V1, V2, and V3")
    void testAllDomainTablesAndRelationshipsExist() {
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'smartprocure' ORDER BY table_name;",
            String.class);

        // V1 Tables
        assertTrue(tables.contains("users"), "Table 'users' must exist in smartprocure");
        assertTrue(tables.contains("purchase_orders"), "Table 'purchase_orders' must exist in smartprocure");
        assertTrue(tables.contains("po_line_items"), "Table 'po_line_items' must exist in smartprocure");
        assertTrue(tables.contains("approval_history"), "Table 'approval_history' must exist in smartprocure");
        assertTrue(tables.contains("flyway_schema_history"), "Table 'flyway_schema_history' must exist in smartprocure");

        // V2 Tables
        assertTrue(tables.contains("departments"), "Table 'departments' must exist in smartprocure");
        assertTrue(tables.contains("department_budgets"), "Table 'department_budgets' must exist in smartprocure");
        assertTrue(tables.contains("suppliers"), "Table 'suppliers' must exist in smartprocure");
        assertTrue(tables.contains("catalog_items"), "Table 'catalog_items' must exist in smartprocure");

        // V3 Tables
        assertTrue(tables.contains("purchase_requests"), "Table 'purchase_requests' must exist in smartprocure");
        assertTrue(tables.contains("pr_line_items"), "Table 'pr_line_items' must exist in smartprocure");
        assertTrue(tables.contains("audit_logs"), "Table 'audit_logs' must exist in smartprocure");
        assertTrue(tables.contains("notifications"), "Table 'notifications' must exist in smartprocure");

        // Verify extended columns in purchase_orders from V3
        List<String> poColumns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'purchase_orders';",
            String.class);
        assertTrue(poColumns.contains("request_id"), "Column 'request_id' must exist on purchase_orders");
        assertTrue(poColumns.contains("supplier_id"), "Column 'supplier_id' must exist on purchase_orders");
        assertTrue(poColumns.contains("department_id"), "Column 'department_id' must exist on purchase_orders");
    }

    @Test
    @DisplayName("Verify legacy public schema tables remain untouched (exactly 7 tables)")
    void testLegacyPublicSchemaPreservation() {
        Integer publicTableCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';", Integer.class);
        assertEquals(7, publicTableCount, "Legacy public schema must retain exactly 7 original tables");
    }
}
