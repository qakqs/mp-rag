package org.rag.config;

import io.micrometer.observation.ObservationRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

@Configuration
public class AiConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }

    @Bean("syncMcpToolCallbackProvider")
    @Primary
    public SyncMcpToolCallbackProvider syncMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
//        mcpClients.remove(0);

        // 用于记录 name 和其对应的 index
        Map<String, Integer> nameToIndexMap = new HashMap<>();
        // 用于记录重复的 index
        Set<Integer> duplicateIndices = new HashSet<>();

        // 遍历 mcpClients 列表
        for (int i = 0; i < mcpClients.size(); i++) {
            String name = mcpClients.get(i).getServerInfo().name();
            if (nameToIndexMap.containsKey(name)) {
                // 如果 name 已经存在，记录当前 index 为重复
                duplicateIndices.add(i);
            } else {
                // 否则，记录 name 和 index
                nameToIndexMap.put(name, i);
            }
        }

        // 删除重复的元素，从后往前删除以避免影响索引
        List<Integer> sortedIndices = new ArrayList<>(duplicateIndices);
        sortedIndices.sort(Collections.reverseOrder());
        for (int index : sortedIndices) {
            mcpClients.remove(index);
        }

        return new SyncMcpToolCallbackProvider(mcpClients);
    }

    @Bean
    public ChatClient chatClient(ChatModel chaModel, @Qualifier("syncMcpToolCallbackProvider") ToolCallbackProvider syncMcpToolCallbackProvider, ChatMemory chatMemory) {
        DefaultChatClientBuilder defaultChatClientBuilder = new DefaultChatClientBuilder(chaModel, ObservationRegistry.NOOP, null, null);
        return defaultChatClientBuilder
                .defaultToolCallbacks(syncMcpToolCallbackProvider)
//       chat:
//        options:
//          model: gpt-4.1 yml配置方式可以注释掉这里的代码。
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .model("gpt-4.1")
//                        .build())
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
    }


}
