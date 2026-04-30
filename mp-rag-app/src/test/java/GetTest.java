import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rag.Application;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * boYdEyx_geVV81wNs412aa6L  aitest
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class GetTest {
    @Resource
    private OllamaChatClient chatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void test() throws Exception {
        String repoURL = "https://gitcode.com/qakqs/api.git";
        String userName = "qakqs";
        String password = "boYdEyx_geVV81wNs412aa6L";

        String lo0calPath = "./cloned-repo";
        FileUtils.deleteDirectory(new File(lo0calPath));
        Git call = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(lo0calPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, password))
                .call();

        call.close();
    }

    @Test
    public void testFile() throws Exception {
        Files.walkFileTree(Paths.get(""),
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        log.info("文件路径:{}", file.toString());
                        PathResource pathResource = new PathResource(file);
                        log.info("");
                        return super.visitFile(file, attrs);
                    }
                }

        );

    }

}
