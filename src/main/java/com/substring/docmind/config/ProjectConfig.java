package com.substring.docmind.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.ModelMap;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration
public class ProjectConfig {

    @Bean
    public static BeanPostProcessor hikariDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof com.zaxxer.hikari.HikariDataSource ds) {
                    String rawUrl = ds.getJdbcUrl();
                    if (rawUrl != null) {
                        try {
                            String url = rawUrl.trim();
                            if (url.startsWith("jdbc:")) {
                                url = url.substring(5);
                            }
                            if (url.startsWith("postgres://")) {
                                url = "postgresql://" + url.substring(11);
                            }
                            if (url.startsWith("postgresql://")) {
                                java.net.URI uri = java.net.URI.create(url);
                                String userInfo = uri.getUserInfo();
                                if (userInfo != null && !userInfo.isEmpty()) {
                                    String[] parts = userInfo.split(":", 2);
                                    if (parts.length > 0 && !parts[0].isEmpty()) {
                                        ds.setUsername(parts[0]);
                                    }
                                    if (parts.length > 1 && !parts[1].isEmpty()) {
                                        ds.setPassword(parts[1]);
                                    }
                                }
                                String host = uri.getHost();
                                int port = uri.getPort();
                                String path = uri.getPath();
                                String query = uri.getQuery();
                                if (query != null) {
                                    query = query.replaceAll("(?i)&?channel_binding=[^&]*", "");
                                    if (query.startsWith("&")) {
                                        query = query.substring(1);
                                    }
                                    if (query.isEmpty()) {
                                        query = "sslmode=require";
                                    }
                                } else {
                                    query = "sslmode=require";
                                }
                                StringBuilder cleanJdbc = new StringBuilder("jdbc:postgresql://").append(host);
                                if (port > 0) {
                                    cleanJdbc.append(":").append(port);
                                }
                                cleanJdbc.append(path).append("?").append(query);
                                ds.setJdbcUrl(cleanJdbc.toString());
                            } else if (!rawUrl.startsWith("jdbc:")) {
                                ds.setJdbcUrl("jdbc:" + rawUrl);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to sanitize JDBC URL {}: {}", rawUrl, e.getMessage());
                        }
                    }
                }
                return bean;
            }
        };
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory memory) {

        log.info("Chatmemory class name {} ", memory.getClass().getName());
        return builder
                .defaultSystem(
                        """
                                                        You are DocMind, an intelligent, versatile, and friendly AI document intelligence assistant.
                                                        Your Capabilities:
                                                        1. Document-Grounded Q&A: When context from the user's uploaded documents is provided, prioritize and base your answer directly on that context, citing document names and page numbers when available.
                                                        2. General Knowledge & Conversation: If the user engages in general conversation (greetings, chit-chat, programming questions, math, explanations, summaries, or general knowledge) that may not be present in the uploaded documents, answer helpfully, accurately, and naturally.
                                                        3. Hybrid Synthesis: If the document context partially covers a topic, synthesize the document facts with your broader knowledge to give a complete, high-quality answer.
                                                        4. Tone & Format: Always be warm, professional, clear, and structured. Use Markdown (headings, bullet points, bold text, code blocks) to make responses easy to read.


                                                        Document Context is :

                                                        {doc_context}

                                                        Produce markdown output.


                                """)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    @Bean
    public OpenAPI openAPI() {

        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("DocMind — AI Document Intelligence & RAG backend")
                                .description(
                                        "REST API for DocMind: Multi-format document ingestion, vector embeddings with PostgreSQL pgvector, and hybrid conversational Q&A with OpenAI.")

                                .version("1.0.0")
                                .contact(new Contact()
                                        .name("Substring Technologies")
                                        .email("support@substringtechnolgoies.com")
                                        .url("https://substringtechnologies.com"))

                ).components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName));

    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Autowired
    public void fixVectorTable(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT atttypmod FROM pg_attribute WHERE attrelid = 'public.vector_store'::regclass AND attname = 'embedding'")) {
                if (rs.next()) {
                    int currentDim = rs.getInt(1);
                    if (currentDim != 1536) {
                        log.info(
                                "Auto-migrating vector_store: current dimension {} does not match 1536. Recreating table...",
                                currentDim);
                        stmt.execute("DROP TABLE IF EXISTS public.vector_store CASCADE");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("vector_store check: {}", e.getMessage());
        }
    }

    @Bean
    public OpenAiHttpClientBuilderCustomizer openAiHttpClientCustomizer() {
        ObjectMapper objectMapper = new ObjectMapper();
        return builder -> builder.interceptor(chain -> {
            var request = chain.request();

            // Force dimensions: 1536 on embedding requests to Gemini (default is 3072,
            // which exceeds pgvector HNSW limit of 2000)
            if (request.url().encodedPath().contains("/embeddings") && request.body() != null) {
                try {
                    okio.Buffer buffer = new okio.Buffer();
                    request.body().writeTo(buffer);
                    String requestBodyStr = buffer.readUtf8();
                    JsonNode reqJson = objectMapper.readTree(requestBodyStr);
                    if (reqJson.isObject()) {
                        ObjectNode reqObj = (ObjectNode) reqJson;
                        if (!reqObj.has("dimensions") || reqObj.get("dimensions").asInt() != 1536) {
                            reqObj.put("dimensions", 1536);
                            RequestBody newReqBody = RequestBody.create(
                                    objectMapper.writeValueAsString(reqObj),
                                    request.body().contentType());
                            request = request.newBuilder().method(request.method(), newReqBody).build();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to set dimensions on embeddings request: {}", e.getMessage());
                }
            }

            var response = chain.proceed(request);

            // Google Gemini OpenAI endpoint omits 'index', 'usage', etc. in embeddings
            // response
            if (request.url().encodedPath().contains("/embeddings") && response.isSuccessful()
                    && response.body() != null) {
                try {
                    String originalBody = response.body().string();
                    JsonNode root = objectMapper.readTree(originalBody);
                    if (root.isObject()) {
                        ObjectNode objRoot = (ObjectNode) root;
                        if (!objRoot.has("object")) {
                            objRoot.put("object", "list");
                        }
                        if (!objRoot.has("model")) {
                            objRoot.put("model", "gemini-embedding-001");
                        }
                        if (!objRoot.has("usage") || objRoot.get("usage").isNull()) {
                            ObjectNode usage = objRoot.putObject("usage");
                            usage.put("prompt_tokens", 0);
                            usage.put("total_tokens", 0);
                        }
                        if (objRoot.has("data") && objRoot.get("data").isArray()) {
                            ArrayNode dataArray = (ArrayNode) objRoot.get("data");
                            for (int i = 0; i < dataArray.size(); i++) {
                                JsonNode item = dataArray.get(i);
                                if (item.isObject()) {
                                    ObjectNode itemObj = (ObjectNode) item;
                                    if (!itemObj.has("index")) {
                                        itemObj.put("index", i);
                                    }
                                    if (!itemObj.has("object")) {
                                        itemObj.put("object", "embedding");
                                    }
                                }
                            }
                        }
                        String modifiedBody = objectMapper.writeValueAsString(objRoot);
                        MediaType contentType = response.body().contentType();
                        ResponseBody newBody = ResponseBody.create(modifiedBody, contentType);
                        return response.newBuilder().body(newBody).build();
                    }
                    MediaType contentType = response.body().contentType();
                    ResponseBody newBody = ResponseBody.create(originalBody, contentType);
                    return response.newBuilder().body(newBody).build();
                } catch (Exception e) {
                    log.warn("Failed to normalize embeddings response: {}", e.getMessage());
                }
            }
            return response;
        });
    }
}
