package com.rk.inventory_management_system.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatRequest {

    private String model;
    private String message;
    private List<Message> history;

}
