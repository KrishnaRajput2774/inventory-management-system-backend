package com.rk.inventory_management_system.controllers;

import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.dtos.ChatBotDtos.MessageResponse;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ModelListResponse;
import com.rk.inventory_management_system.services.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());

        try {
            ChatResponse response = chatbotService.processMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            ChatResponse errorResponse = ChatResponse.builder()
                    .message("I apologize, but I encountered an error processing your request. Please try again.")
                    .model(request.getModel())
                    .timestamp(System.currentTimeMillis())
                    .error(true)
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request: {} (model: {})", request.getMessage(), request.getModel());
        StringBuilder stringBuilder = new StringBuilder("\nResponse: \n");
        return chatbotService.processMessageStream(request)
                .doOnNext(stringBuilder::append)
//                .map(tokenList -> "data: " + String.join("", tokenList) + "\n\n")
//                .map(token->"data: "+token+"\n\n")
                .doOnError(error -> log.error("Error in streaming chat", error))
                .doOnComplete(() -> log.info("Streaming chat completed {}",stringBuilder.toString()))
                .onErrorResume(error -> Flux.just("data: Error occurred. Please try again.\n\n"))
                .concatWith(Flux.just("[DONE]\n\n"));
    }

    @PostMapping(value = "/chat/stream-json", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ChatResponse> chatStreamJson(@Valid @RequestBody ChatRequest request) {
        log.info("Received JSON streaming chat request: {} (model: {})", request.getMessage(), request.getModel());
        StringBuilder stringBuilder = new StringBuilder(" ");
        return chatbotService.processMessageStreamJson(request)
                .doOnNext(chatResponse -> stringBuilder.append(chatResponse.getMessage()))
                .doOnError(error -> log.error("Error in JSON streaming chat", error))
                .doOnComplete(() -> log.info("JSON streaming chat completed: {}",stringBuilder.toString()))
                .onErrorResume(error -> {
                    ChatResponse errorResponse = ChatResponse.builder()
                            .message("Error occurred while processing your request")
                            .model(request.getModel())
                            .timestamp(System.currentTimeMillis())
                            .error(true)
                            .build();
                    return Flux.just(errorResponse);
                });
    }

    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> getAvailableModels() {
        try {
            ModelListResponse response = new ModelListResponse(
                    chatbotService.getAvailableModels(),
                    true
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving available models", e);
            ModelListResponse errorResponse = new ModelListResponse(null, false);
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> healthCheck() {
        return ResponseEntity.ok(new MessageResponse("Chatbot service is running", true));
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<MessageResponse> clearCache() {
        try {
            chatbotService.clearCache();
            return ResponseEntity.ok(new MessageResponse("Cache cleared successfully", true));
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return ResponseEntity.ok(new MessageResponse("Error clearing cache", false));
        }
    }

    @GetMapping(value = "/test-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStream() {
        return Flux.just("Hello", " ", "from", " ", "streaming", " ", "endpoint!")
                .concatWith(Flux.just("data: [DONE]\n\n"));
    }

    @GetMapping(value = "/test-stream-enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStreamEnhanced() {
        return Flux.concat(
                Flux.just("event: start\ndata: {\"message\":\"Starting test stream\"}\n\n"),
                Flux.just("Test", " ", "streaming", " ", "with", " ", "events!")
                        .delayElements(Duration.ofMillis(300))
                        .map(token -> "event: token\ndata: " + token + "\n\n"),
                Flux.just("event: complete\ndata: {\"message\":\"Stream completed\"}\n\n")
        );
    }

    @GetMapping("/status")
    public ResponseEntity<MessageResponse> getStatus() {
        // You could add metrics here like active streams, cache size, etc.
        return ResponseEntity.ok(new MessageResponse("Service is operational", true));
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}