package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.services.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IntentClassificationService {

    // Database query indicators
    private static final List<String> DB_KEYWORDS = Arrays.asList(
            "find", "search", "get", "show", "list", "display", "retrieve",
            "product", "customer", "order", "supplier", "category", "inventory",
            "stock", "price", "sales", "revenue", "top", "low", "quantity",
            "named", "called", "with", "by", "from", "in", "containing",
            "total", "count", "sum", "average", "maximum", "minimum",
            "recent", "last", "today", "yesterday", "week", "month", "year",
            "status", "pending", "completed", "cancelled"
    );

    // Simple chat indicators
    private static final List<String> CHAT_KEYWORDS = Arrays.asList(
            "hello", "hi", "help", "what can you do", "capabilities",
            "how are you", "thanks", "thank you", "goodbye", "bye",
            "explain", "tell me about", "what is", "how does",
            "recommend", "suggest", "advice", "opinion"
    );

    // SQL generation patterns
    private static final List<Pattern> SQL_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(find|search|get|show|list)\\s+.*\\b(product|customer|order|supplier)s?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(low|high)\\s+stock\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btop\\s+\\d+\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(sales|revenue|orders?)\\s+(last|this|in)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnamed?\\s+[\"']?\\w+[\"']?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bprice\\s+(between|above|below|over|under)\\b", Pattern.CASE_INSENSITIVE)
    );



    public IntentType classifyIntent(String message) {
        String normalizedMessage = message.toLowerCase().trim();

        log.debug("Classifying intent for message: {}", message);

        // Check for simple chat patterns first
        if (isSimpleChat(normalizedMessage)) {
            log.debug("Classified as SIMPLE_CHAT");
            return IntentType.SIMPLE_CHAT;
        }

        // Check for SQL-able database queries
        if (isDatabaseQuery(normalizedMessage)) {
            log.debug("Classified as DATABASE_QUERY");
            return IntentType.DATABASE_QUERY;
        }

        // Default to complex analysis for everything else
        log.debug("Classified as COMPLEX_ANALYSIS");
        return IntentType.COMPLEX_ANALYSIS;
    }

    private boolean isSimpleChat(String message) {
        // Check for greeting/help patterns
        for (String keyword : CHAT_KEYWORDS) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        // Check for questions about the system itself
        if (message.contains("what can you") || message.contains("your capabilities") ||
                message.contains("help me") || message.contains("how do i")) {
            return true;
        }

        return false;
    }

    private boolean isDatabaseQuery(String message) {
        // Check SQL generation patterns
        for (Pattern pattern : SQL_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        // Check for database keywords combination
        long dbKeywordCount = DB_KEYWORDS.stream()
                .mapToLong(keyword -> message.contains(keyword) ? 1 : 0)
                .sum();

        // If message has 2+ database keywords, likely a DB query
        return dbKeywordCount >= 2;
    }

    public String getIntentDescription(IntentType intent) {
        return switch (intent) {
            case DATABASE_QUERY -> "Database query requiring SQL generation";
            case SIMPLE_CHAT -> "Simple conversation requiring no database access";
            case COMPLEX_ANALYSIS -> "Complex analysis requiring tools and processing";
        };
    }
}