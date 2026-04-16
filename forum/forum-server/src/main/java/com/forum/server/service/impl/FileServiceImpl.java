package com.forum.server.service.impl;

import com.forum.common.constant.MessageConstant;
import com.forum.common.exception.BaseException;
import com.forum.server.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Value("${forum.upload.path}")
    private String uploadPath;

    @Value("${forum.upload.url-prefix}")
    private String urlPrefix;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        // 文件大小限制 (5MB) - Spring Boot default config may also block before reaching here
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BaseException(MessageConstant.FILE_TOO_LARGE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        // 验证后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }

        // 按日期建目录：yyyyMMdd
        String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File dir = new File(uploadPath + dateDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Failed to create directory: {}", dir.getAbsolutePath());
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        // 重命名文件
        String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;
        File dest = new File(dir, newFilename);

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("File upload error", e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        // 返回 URL
        return urlPrefix + dateDir + "/" + newFilename;
    }
}
