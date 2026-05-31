package org.rag.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.rag.api.IRAGService;
import org.rag.resp.Response;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController()
@RequestMapping("/api/v1/rag/")
public class RagController implements IRAGService {

    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore pgVectorStore;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @RequestMapping(value = "queryRagTagList", method = RequestMethod.POST)
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("ragTag");
        return Response.<List<String>>builder()
                .code("0000")
                .info("调用成功")
                .data(new ArrayList<>(elements))
                .build();
    }

    @Override
    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    public Response<String> uploadFile(@RequestParam("ragTag") String ragTag, @RequestParam("files") List<MultipartFile> files) {

        log.info("上传知识库开始 {}, 收到文件数: {}", ragTag, files.size());
        for (MultipartFile file : files) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

            // 添加知识库标签
            documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));

            pgVectorStore.accept(documentSplitterList);

            // 添加知识库记录
            RList<String> elements = redissonClient.getList("ragTag");
            if (!elements.contains(ragTag)) {
                elements.add(ragTag);
            }
        }

        log.info("上传知识库完成 {}", ragTag);
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    @Override
    @RequestMapping(value = "analyzeGitRepository", method = RequestMethod.POST)
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl, @RequestParam("userName") String userName, @RequestParam("token") String token) throws Exception {
        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());
        FileUtils.deleteDirectory(new File(localPath));

        // Clash proxy for JGit
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .call();
        try {
            // 使用Files.walkFileTree遍历目录
            Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                    // 定义需要排除的图片文件扩展名列表
                    final List<String> IMAGE_EXTENSIONS = Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tiff");

                    try {
                        // 1. 排除 .git 目录下的文件
                        String pathString = file.toString();
                        if (pathString.contains(File.separator + ".git" + File.separator) || pathString.endsWith(".git")) {
                            log.warn("跳过文件: {}", pathString);
                            return FileVisitResult.CONTINUE; // 跳过当前文件
                        }

                        // 2. 排除图片文件
                        String lowerCasePath = pathString.toLowerCase();
                        if (IMAGE_EXTENSIONS.stream().anyMatch(lowerCasePath::endsWith)) {
                            log.warn("跳过图片文件: {}", pathString);
                            return FileVisitResult.CONTINUE;
                        }

                        TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                        List<Document> documents = reader.get();
                        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

                        documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

                        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

                        pgVectorStore.accept(documentSplitterList);
                    } catch (Exception e) {
                        log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                        throw e;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
            git.close();
            FileUtils.deleteDirectory(new File(localPath));
            // 添加知识库记录
            RList<String> elements = redissonClient.getList("ragTag");
            if (!elements.contains(repoProjectName)) {
                elements.add(repoProjectName);
            }
        } catch (Exception e) {
            log.error("", e);
            return Response.<String>builder().code("0000").info("失败").build();
        } finally {
            git.close();
            FileUtils.deleteDirectory(new File(localPath));
        }
        log.info("遍历解析路径，上传完成:{}", repoUrl);
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
        for (int i = 0; i < 10000; i++) {
            m.put("ragTag" + i, String.valueOf(i));
        }
        m.put("ragTag", String.valueOf("s"));

        System.out.println(m);
    }

}
