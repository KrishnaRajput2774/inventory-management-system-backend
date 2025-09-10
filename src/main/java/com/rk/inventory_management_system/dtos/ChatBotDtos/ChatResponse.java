package com.rk.inventory_management_system.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatResponse {
    private String model;
    private String message;
}
