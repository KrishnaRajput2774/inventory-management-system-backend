package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.services.ChatService;
import com.rk.inventory_management_system.services.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ModelRegistry modelRegistry;
    private final ChatService chatService;


    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest chatRequest) {

        if(!modelRegistry.isValidModel(chatRequest.getModel())) {
            return ResponseEntity.badRequest().body("Invalid Model Selected");
        }
        if(!chatRequest.getModel().equals("phi4-mini:latest") && !chatRequest.getModel().equals("llama3")) {
            return ResponseEntity.badRequest().body("Other models are yet to be integrate");
        }

        ChatResponse chatResponse = chatService.chat(chatRequest);

        return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/models")
    public ResponseEntity<List<String>> getModels() {
        return ResponseEntity.ok(modelRegistry.getAvailableModels());
    }



}
