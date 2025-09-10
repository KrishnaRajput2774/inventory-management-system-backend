package com.rk.inventory_management_system.services;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

@Component
public class ModelRegistry {

    private final List<String> models = List.of("llama3","mistral", "gemma","phi4-mini:latest");

    public List<String> getAvailableModels() {
        return models;
    }

    public boolean isValidModel(String model) {
        return models.contains(model);
    }




}
