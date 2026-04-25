package com.forum.server.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 上传文件到 OSS
     * @param file 文件实体
     * @return 访问 URL 路径
     */
    String uploadFile(MultipartFile file);
}
