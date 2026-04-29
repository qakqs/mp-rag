package org.rag.http;

import jakarta.annotation.Resource;
import org.rag.api.IAiService;
import org.rag.req.GenerateRequest;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.RequestBody;
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
    @RequestMapping(value = "generate", method = RequestMethod.POST)
    public ChatResponse generate(@RequestBody GenerateRequest request) {
        return ollamaChatClient.call(new Prompt(request.getMessage(), OllamaOptions.create().withModel(request.getModel())));
    }

    @Override
    @RequestMapping(value = "generateStream", method = RequestMethod.POST)
    public Flux<ChatResponse> generateStream(@RequestBody GenerateRequest request) {
        return ollamaChatClient.stream(new Prompt(request.getMessage(), OllamaOptions.create().withModel(request.getModel())));
    }

}
