package org.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Test {

    @Resource
    private ChatClient.Builder builder;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @org.junit.Test
    public void test_tool() {
        ChatClient client = builder.defaultToolCallbacks(toolCallbackProvider)
                .defaultOptions(ChatOptions.builder().model("qwen3-max").build())
                .build();

        String userInput = "有哪些工具可以使用";
        System.out.println(client.prompt(userInput).call().content());

    }


}
