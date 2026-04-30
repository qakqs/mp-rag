package org.rag.api;

import org.rag.resp.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRAGService {
    Response<List<String>> queryRagTagList();

    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

}
