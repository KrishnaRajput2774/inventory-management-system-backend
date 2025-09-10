package com.rk.inventory_management_system.util;

import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.dtos.ChatBotDtos.Message;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {


    public String buildPrompt(ChatRequest chatRequest) {
        StringBuilder sb = new StringBuilder();

        sb.append("System: You are an AI assistant for Inventory Management System\n");

        if(chatRequest.getHistory() != null) {
            for(Message message : chatRequest.getHistory()) {
                sb.append(message.getRole()).append(": ")
                        .append(message.getContent()).append("\n");
            }
        }

        sb.append("User: ").append(chatRequest.getMessage()).append("\n");
        return sb.toString();
    }


}
