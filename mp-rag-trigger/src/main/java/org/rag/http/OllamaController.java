package org.rag.http;

import jakarta.annotation.Resource;
import org.rag.api.IAiService;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ollama")
public class OllamaController implements IAiService {

    @Resource
    private OllamaChatClient ollamaChatClient;

    @Override
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    public ChatResponse generate(String model, String message) {
        return ollamaChatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }

    @Override
    @RequestMapping(value = "generateStream", method = RequestMethod.GET)
    public Flux<ChatResponse> generateStream(String model, String message) {
        return ollamaChatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }

}
