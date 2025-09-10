package com.rk.inventory_management_system.services.impl;

import com.rk.inventory_management_system.client.OllamaClient;
import com.rk.inventory_management_system.config.RestClientConfig;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.services.ChatService;
import com.rk.inventory_management_system.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final RestClient restClient;
    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {

        String finalPrompt = promptBuilder.buildPrompt(chatRequest);

         String answer = ollamaClient.generate(chatRequest.getModel(), finalPrompt);
        return new ChatResponse(chatRequest.getModel(),answer);
    }
}
