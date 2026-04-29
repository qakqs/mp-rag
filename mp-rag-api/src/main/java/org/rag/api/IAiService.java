package org.rag.api;

import org.rag.req.GenerateRequest;
import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

public interface IAiService {


    ChatResponse generate(GenerateRequest request);

    Flux<ChatResponse> generateStream(GenerateRequest request);

}
