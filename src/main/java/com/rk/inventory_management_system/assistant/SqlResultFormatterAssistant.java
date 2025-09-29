package com.rk.inventory_management_system.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SqlResultFormatterAssistant {

    @SystemMessage("""
        You are an inventory management assistant specialized in presenting database query results
        in a clear, user-friendly format.
        
        Your task is to take raw database results and present them in a natural, conversational way.
        
        Guidelines:
        - Present data in an organized, easy-to-read format
        - Use bullet points or numbered lists when appropriate
        - If results show problems (low stock, etc.), mention them prominently
        - Always end with an offer to help further
        
        Database Results: {{results}}
        Original Question: {{question}}
        
        Please format these database results in a user-friendly way.
        """)
    TokenStream formatAndStream(
            @dev.langchain4j.service.UserMessage String userMessage,
            @V("results") String results,
            @V("question") String question
    );
}