package com.rk.inventory_management_system.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import dev.langchain4j.model.chat.ChatLanguageModel;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqlGenerationService {

    private final ChatLanguageModel sqlGenerationModel;
    private final DataSource readOnlyDataSource; // Use read-only datasource

    private static final String SQL_GENERATION_PROMPT = """
        You are an expert SQL generator for an inventory management system.
        
        Database Schema:
        - customer (id, name, contact_number, email, address, created_at)
        - supplier (id, name, contact_number, email, address, created_at)  
        - product_category (id, name, created_date, description)
        - product (id, product_code, attribute, name, brand_name, description, actual_price, selling_price, discount, stock_quantity, quantity_sold, category_id, supplier_id, low_stock_threshold)
        - order (id, created_at, updated_at, completed_at, order_type, order_status, customer_id, supplier_id, total_price, payment_type)
        - order_item (id, order_date, product_id, order_id, price_at_order_time, quantity)
        
        Rules:
        1. Generate ONLY valid PostgreSQL SELECT statements
        2. Always use proper JOINs when accessing related tables
        3. Use LIMIT clause for large result sets (max 50 rows)
        4. Use ILIKE for case-insensitive text searches
        5. For date queries, use proper PostgreSQL date functions
        6. Return only the SQL query, no explanations
        7. Use table aliases for better readability
        
        User Question: %s
        
        SQL Query:
        """;

    public String generateSql(String userQuestion) {
        try {
            log.info("Generating SQL for question: {}", userQuestion);

            String prompt = String.format(SQL_GENERATION_PROMPT, userQuestion);
            String generatedSql = sqlGenerationModel.generate(prompt);

            // Clean and validate the SQL
            String cleanedSql = cleanSqlQuery(generatedSql);
            log.info("Generated SQL: {}", cleanedSql);

            return cleanedSql;

        } catch (Exception e) {
            log.error("Error generating SQL for question: {}", userQuestion, e);
            throw new RuntimeException("Failed to generate SQL query", e);
        }
    }

    public List<Map<String, Object>> executeSql(String sql) {
        try {
            log.info("Executing SQL: {}", sql);

            // Validate SQL is a SELECT statement
            if (!isValidSelectQuery(sql)) {
                throw new IllegalArgumentException("Only SELECT queries are allowed");
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            log.info("SQL execution returned {} rows", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error executing SQL: {}", sql, e);
            throw new RuntimeException("Failed to execute SQL query: " + e.getMessage(), e);
        }
    }

    public String formatResultsForAI(List<Map<String, Object>> results, String originalQuestion) {
        if (results.isEmpty()) {
            return "No results found for the query.";
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append(String.format("Query Results (%d rows):\n\n", results.size()));

        // Format results as structured text
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> row = results.get(i);
            formatted.append(String.format("Result %d:\n", i + 1));

            row.forEach((key, value) -> {
                formatted.append(String.format("  %s: %s\n",
                        formatColumnName(key),
                        value != null ? value.toString() : "N/A"));
            });
            formatted.append("\n");
        }

        formatted.append("Original Question: ").append(originalQuestion);

        return formatted.toString();
    }

    private String cleanSqlQuery(String sql) {
        // Remove common prefixes/suffixes that LLM might add
        sql = sql.trim();

        // Remove markdown code blocks
        sql = sql.replaceAll("^```sql\\s*", "").replaceAll("\\s*```$", "");
        sql = sql.replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");

        // Remove any explanatory text before/after
        String[] lines = sql.split("\\n");
        StringBuilder cleanSql = new StringBuilder();

        boolean inQuery = false;
        for (String line : lines) {
            line = line.trim();
            if (line.toUpperCase().startsWith("SELECT")) {
                inQuery = true;
            }
            if (inQuery) {
                cleanSql.append(line).append(" ");
            }
            if (line.endsWith(";")) {
                break;
            }
        }

        return cleanSql.toString().trim();
    }

    private boolean isValidSelectQuery(String sql) {
        String upperSql = sql.toUpperCase().trim();

        // Must start with SELECT
        if (!upperSql.startsWith("SELECT")) {
            return false;
        }

        // Must not contain dangerous keywords
        String[] forbiddenKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE"};
        for (String keyword : forbiddenKeywords) {
            if (upperSql.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    private String formatColumnName(String columnName) {
        // Convert snake_case to Title Case
        Pattern pattern = Pattern.compile("\\b\\w");
        Matcher matcher = pattern.matcher(columnName.replace("_", " ").toLowerCase());
        return matcher.replaceAll(match -> match.group().toUpperCase());
    }
}