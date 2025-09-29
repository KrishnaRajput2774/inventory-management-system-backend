package com.rk.inventory_management_system.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SimpleStreamingAssistant {

    @SystemMessage("""
        You are a helpful inventory management assistant. You can provide general information
        and guidance about inventory management, but for specific data queries, you'll need
        to redirect users to use the appropriate search functions.
        
        Keep responses conversational, helpful, and professional. If asked for specific data
        that requires database access, politely explain that you can help with that if they
        ask more specifically about products, customers, orders, or suppliers.
        """)
    TokenStream chatStream(@UserMessage String message);
}