package com.rk.inventory_management_system.services;

import com.rk.inventory_management_system.assistant.InventoryAssistant;
import com.rk.inventory_management_system.assistant.SimpleStreamingAssistant;
import com.rk.inventory_management_system.assistant.SqlResultFormatterAssistant;
import com.rk.inventory_management_system.config.LangChain4JConfig;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatRequest;
import com.rk.inventory_management_system.dtos.ChatBotDtos.ChatResponse;
import com.rk.inventory_management_system.services.enums.IntentType;
import com.rk.inventory_management_system.tools.InventoryTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

        private final LangChain4JConfig.ModelFactory modelFactory;
        private final InventoryTools inventoryTools;
        private final ChatLanguageModel defaultChatModel;
        private final StreamingChatLanguageModel defaultStreamingChatModel;
        private final IntentClassificationService intentClassifier;
        private final SqlGenerationService sqlService;

        @Value("${chatbot.memory.window-size:20}")
        private int memoryWindowSize;

        @Value("${ollama.available-models:llama3.1,phi4-mini}")
        private String availableModelsString;

        // Caches for different assistant types
        private final Map<String, InventoryAssistant> assistantCache = new ConcurrentHashMap<>();
        private final Map<String, SimpleStreamingAssistant> simpleStreamingCache = new ConcurrentHashMap<>();
        private final Map<String, SqlResultFormatterAssistant> sqlFormatterCache = new ConcurrentHashMap<>();

        private List<String> availableModels;

        @PostConstruct
        public void init() {
                availableModels = Arrays.asList(availableModelsString.split(","));
                log.info("Initialized chatbot service with models: {}", availableModels);
        }

        public ChatResponse processMessage(ChatRequest request) {
                long startTime = System.currentTimeMillis();

                try {
                        InventoryAssistant assistant = getOrCreateAssistant(request.getModel());
                        String response = assistant.chat(request.getMessage());

                        long processingTime = System.currentTimeMillis() - startTime;
                        log.info("Processed message in {}ms using model: {}", processingTime, request.getModel());

                        return ChatResponse.builder()
                                .message(response)
                                .model(request.getModel())
                                .timestamp(System.currentTimeMillis())
                                .processingTimeMs(processingTime)
                                .error(false)
                                .build();
                } catch (Exception e) {
                        log.error("Error processing message with model: {}", request.getModel(), e);
                        long processingTime = System.currentTimeMillis() - startTime;

                        return ChatResponse.builder()
                                .message("I apologize, but I encountered an error processing your request. Please try again.")
                                .model(request.getModel())
                                .timestamp(System.currentTimeMillis())
                                .processingTimeMs(processingTime)
                                .error(true)
                                .build();
                }
        }

        /**
         * NEW: Hybrid streaming approach with intent classification
         */
        public Flux<String> processMessageStream(ChatRequest request) {
                log.info("Processing streaming message with model: {}", request.getModel());

                // Classify the intent first
                IntentType intent = intentClassifier.classifyIntent(request.getMessage());
                log.info("Classified intent as: {} for message: {}", intent, request.getMessage());

                return switch (intent) {
                        case SIMPLE_CHAT -> handleSimpleStreaming(request);
                        case DATABASE_QUERY -> handleDatabaseQueryStreaming(request);
                        case COMPLEX_ANALYSIS -> handleComplexAnalysisStreaming(request);
                };
        }

        private Flux<String> handleSimpleStreaming(ChatRequest request) {
                log.info("Handling simple streaming for: {}", request.getMessage());

                return Flux.<String>create(sink -> {
                        try {
                                SimpleStreamingAssistant assistant = getOrCreateSimpleStreamingAssistant(request.getModel());
                                TokenStream tokenStream = assistant.chatStream(request.getMessage());

                                tokenStream
                                        .onNext(sink::next)
                                        .onComplete(response -> {
                                                log.info("Simple streaming completed");
                                                sink.complete();
                                        })
                                        .onError(throwable -> {
                                                log.error("Error in simple streaming", throwable);
                                                sink.error(throwable);
                                        })
                                        .start();

                        } catch (Exception e) {
                                log.error("Error in simple streaming setup", e);
                                sink.error(e);
                        }
                });
        }

        /**
         * Handle database queries with SQL generation + streaming formatting
         */
        private Flux<String> handleDatabaseQueryStreaming(ChatRequest request) {
                log.info("Handling database query streaming for: {}", request.getMessage());

                return Flux.<String>create(sink -> {
                        try {
                                // Step 1: Generate and execute SQL
                                sink.next("🔍 Searching database... ");

                                String sql = sqlService.generateSql(request.getMessage());
                                List<Map<String, Object>> results = sqlService.executeSql(sql);
                                String formattedResults = sqlService.formatResultsForAI(results, request.getMessage());

                                sink.next("✅ Found results! ");

                                // Step 2: Stream the AI-formatted response
                                SqlResultFormatterAssistant formatter = getOrCreateSqlFormatterAssistant(request.getModel());
                                TokenStream tokenStream = formatter.formatAndStream(
                                        "Please format these database results: " + request.getMessage(),
                                        formattedResults,
                                        request.getMessage()
                                );

                                tokenStream
                                        .onNext(sink::next)
                                        .onComplete(response -> {
                                                log.info("Database query streaming completed");
                                                sink.complete();
                                        })
                                        .onError(throwable -> {
                                                log.error("Error in database query streaming", throwable);
                                                sink.error(throwable);
                                        })
                                        .start();

                        } catch (Exception e) {
                                log.error("Error in database query processing", e);
                                sink.next("❌ Sorry, I encountered an error while searching the database. ");
                                sink.error(e);
                        }
                });
        }

        private Flux<String> handleComplexAnalysisStreaming(ChatRequest request) {
                log.info("Handling complex analysis streaming for: {}", request.getMessage());

                return Flux.<String>create(sink -> {
                        try {
                                // Execute synchronously with tools
                                sink.next("🤔 Analyzing your request... ");

                                InventoryAssistant assistant = getOrCreateAssistant(request.getModel());
                                String response = assistant.chat(request.getMessage());

                                // Simulate streaming by chunking the response
                                String[] chunks = chunkResponse(response);
                                for (String chunk : chunks) {
                                        sink.next(chunk);
                                        // Small delay to simulate streaming
                                        Thread.sleep(50);
                                }

                                sink.complete();

                        } catch (Exception e) {
                                log.error("Error in complex analysis streaming", e);
                                sink.error(e);
                        }
                });
        }

        /**
         * Chunk response for fake streaming
         */
        private String[] chunkResponse(String response) {
                // Split by sentences and words for natural chunking
                String[] sentences = response.split("(?<=[.!?])\\s+");
                return sentences;
        }

        // Assistant creation methods
        private InventoryAssistant getOrCreateAssistant(String modelName) {
                return assistantCache.computeIfAbsent(modelName, this::createAssistant);
        }

        private SimpleStreamingAssistant getOrCreateSimpleStreamingAssistant(String modelName) {
                return simpleStreamingCache.computeIfAbsent(modelName, this::createSimpleStreamingAssistant);
        }

        private SqlResultFormatterAssistant getOrCreateSqlFormatterAssistant(String modelName) {
                return sqlFormatterCache.computeIfAbsent(modelName, this::createSqlFormatterAssistant);
        }

        private InventoryAssistant createAssistant(String modelName) {
                log.info("Creating new assistant for model: {}", modelName);

                ChatLanguageModel chatModel = (modelName != null && !modelName.isEmpty())
                        ? modelFactory.getChatModel(modelName)
                        : defaultChatModel;

                return AiServices.builder(InventoryAssistant.class)
                        .chatLanguageModel(chatModel)
                        .tools(inventoryTools)
                        .chatMemory(MessageWindowChatMemory.withMaxMessages(memoryWindowSize))
                        .build();
        }

        private SimpleStreamingAssistant createSimpleStreamingAssistant(String modelName) {
                log.info("Creating simple streaming assistant for model: {}", modelName);

                StreamingChatLanguageModel streamingModel = (modelName != null && !modelName.isEmpty())
                        ? modelFactory.getStreamingChatModel(modelName)
                        : defaultStreamingChatModel;

                return AiServices.builder(SimpleStreamingAssistant.class)
                        .streamingChatLanguageModel(streamingModel)
                        .build(); // No tools for simple streaming
        }

        private SqlResultFormatterAssistant createSqlFormatterAssistant(String modelName) {
                log.info("Creating SQL formatter assistant for model: {}", modelName);

                StreamingChatLanguageModel streamingModel = (modelName != null && !modelName.isEmpty())
                        ? modelFactory.getStreamingChatModel(modelName)
                        : defaultStreamingChatModel;

                return AiServices.builder(SqlResultFormatterAssistant.class)
                        .streamingChatLanguageModel(streamingModel)
                        .build(); // No tools, just formatting
        }

        // Keep existing JSON streaming method for compatibility
        public Flux<ChatResponse> processMessageStreamJson(ChatRequest request) {
                return processMessageStream(request)
                        .map(token -> ChatResponse.builder()
                                .message(token)
                                .model(request.getModel())
                                .timestamp(System.currentTimeMillis())
                                .isStreaming(true)
                                .error(false)
                                .build());
        }

        public List<String> getAvailableModels() {
                return availableModels;
        }

        public void clearCache() {
                assistantCache.clear();
                simpleStreamingCache.clear();
                sqlFormatterCache.clear();
                log.info("Cleared all assistant caches");
        }
}