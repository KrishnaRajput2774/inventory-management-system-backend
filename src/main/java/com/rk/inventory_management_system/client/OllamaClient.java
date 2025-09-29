package com.rk.inventory_management_system.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient restClient;
    private final WebClient webClient;

    private final ObjectMapper objectMapper;

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

    public Flux<String> generateStream(String model, String prompt) {

        Map<String, Object> body = new HashMap<>();
        body.put("model",model);
        body.put("prompt",prompt);
        body.put("stream",true);

        return webClient.post()
                .uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk->{
                    try{
                        JsonNode root = objectMapper.readTree(chunk);
                        log.info(root.path("response").asText(""));
                        return root.path("response").asText("");
                    }catch (Exception e) {
                        return "";
                    }
                });
    }
}