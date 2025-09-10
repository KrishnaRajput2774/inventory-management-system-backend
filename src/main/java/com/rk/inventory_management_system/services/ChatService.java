package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;

public interface ChatService {

        public ChatResponse chat(ChatRequest chatRequest);
}
