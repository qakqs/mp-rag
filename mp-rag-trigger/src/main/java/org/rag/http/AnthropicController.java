package org.rag.http;

import jakarta.annotation.Resource;
import org.rag.req.GenerateRequest;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/anthropic/")
public class AnthropicController {

    @Resource
    private ChatModel chatModel;

    @Resource
    private VectorStore vectorStore;

    @PostMapping("generate")
    public Map<String, Object> generate(@RequestBody GenerateRequest request) {
        String content = chatModel.call(request.getMessage());
        return Map.of("model", request.getModel(), "content", content);
    }

    @GetMapping("generate_stream")
    public Flux<ChatResponse> generateStream(@RequestParam String model,
                                             @RequestParam String message) {
        return chatModel.stream(new Prompt(message));
    }

    @PostMapping("generate_stream_rag")
    public Flux<ChatResponse> generateStreamRag(@RequestBody GenerateRequest request) {
        if (null == request.getRagTag()) {
            Prompt prompt = new Prompt(new UserMessage(request.getMessage()));
            return chatModel.stream(prompt);

        }
        String context = searchContext(request.getMessage(), request.getRagTag());
        SystemMessage system = new SystemMessage("""
                1. Don't provide any information that is not related to the question, and don't output any duplicate content; 
                2. Avoid using "context-based..." or "The provided information..." said; 
                3. Your answers must be correct, accurate, and written in an expertly unbiased and professional tone; 
                4. The appropriate text structure in the answer is determined according to the characteristics of the content, please include subheadings in the output to improve readability; 
                5. When generating a response, provide a clear conclusion or main idea first, and do not need to have a title; 
                6. Make sure each section has clear subheadings so that users can better understand and reference your output; 
                7. If the information is complex or contains multiple sections, make sure each section has an appropriate heading to create a hierarchical structure.
                8. Your reply must be in Chinese!
                9. You are AI partaner
                
                                DOCUMENTS:
                                %s
                """.formatted(context));

        Prompt prompt = new Prompt(List.of(system, new UserMessage(request.getMessage())));
        return chatModel.stream(prompt);
    }

    private static final int MAX_CONTEXT_CHARS = 100_000;

    private String searchContext(String query, String ragTag) {
        SearchRequest req = SearchRequest.builder()
                .query(query)
                .topK(1)
                .filterExpression("knowledge == '" + ragTag + "'")
                .build();
        List<Document> docs = vectorStore.similaritySearch(req);
        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            String text = doc.getText();
            if (sb.length() + text.length() > MAX_CONTEXT_CHARS) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(text);
        }
        return sb.toString();
    }
}
