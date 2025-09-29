package com.rk.inventory_management_system.dtos.ChatBotDtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 5000, message = "Message too long")
    private String message;

    @Builder.Default
    private String model = "llama3.1";

    private String conversationId;

    @JsonProperty("streaming")
    @Builder.Default
    private boolean streaming = false;

    private String context;

    @Builder.Default
    private double temperature = 0.7;

    @Builder.Default
    private int maxTokens = 2000;
}