package com.rk.inventory_management_system.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.inventory_management_system.config.RestClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient restClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String URL = "api/generate";

    public String generate(String model, String prompt) {

        Map<String,Object> body = new HashMap<>();
        body.put("model",model);
        body.put("prompt",prompt);
        body.put("stream",false);

        ResponseEntity<String> response = restClient.post()
                .uri(URL)
                .body(body)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("response").asText();
        }catch (Exception e) {
            throw new RuntimeException("Error parsing Ollama response");
        }


    }

}
