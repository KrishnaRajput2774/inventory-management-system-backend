package com.rk.inventory_management_system.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
public class LangChain4JConfig {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.default-model:llama3.1}")
    private String defaultModel;

    @Value("${ollama.timeout:300}")
    private long timeoutSeconds;

    @Bean
    @Primary
    public ChatLanguageModel primaryChatModel() {
        return createChatLanguageModel(defaultModel);
    }

    @Bean
    @Primary
    public StreamingChatLanguageModel primaryStreamingChatModel() {
        return createStreamingChatLanguageModel(defaultModel);
    }

    @Bean
    public ModelFactory modelFactory() {
        return new ModelFactory();
    }

    private ChatLanguageModel createChatLanguageModel(String modelName) {
        log.info("Creating chat model for: {}", modelName);
        return OllamaChatModel.builder()
                .modelName(modelName)
                .baseUrl(ollamaBaseUrl)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .temperature(0.5)
                .topK(25)
                .topP(0.7)
                .build();
    }

    private StreamingChatLanguageModel createStreamingChatLanguageModel(String modelName) {
        log.info("Creating streaming chat model for: {}", modelName);
        return OllamaStreamingChatModel.builder()
                .modelName(modelName)
                .baseUrl(ollamaBaseUrl)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .temperature(0.5)
                .topK(25)
                .topP(0.7)
                .build();
    }

    public class ModelFactory {
        private final Map<String, ChatLanguageModel> chatModels = new HashMap<>();
        private final Map<String, StreamingChatLanguageModel> streamingModels = new HashMap<>();

        public ChatLanguageModel getChatModel(String modelName) {
            return chatModels.computeIfAbsent(modelName, this::createChatModelInstance);
        }

        public StreamingChatLanguageModel getStreamingChatModel(String modelName) {
            return streamingModels.computeIfAbsent(modelName, this::createStreamingChatModelInstance);
        }

        private ChatLanguageModel createChatModelInstance(String modelName) {
            return createChatLanguageModel(modelName);
        }

        private StreamingChatLanguageModel createStreamingChatModelInstance(String modelName) {
            return createStreamingChatLanguageModel(modelName);
        }
    }
}
