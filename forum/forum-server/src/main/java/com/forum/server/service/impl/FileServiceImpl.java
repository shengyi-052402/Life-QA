package com.forum.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.forum.common.constant.MessageConstant;
import com.forum.common.exception.BaseException;
import com.forum.server.config.OssConfig;
import com.forum.server.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);
        validateOssConfig();

        String suffix = extractSuffix(file.getOriginalFilename());
        String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String objectKey = "images/" + dateDir + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        return uploadToOss(file, objectKey);
    }

    private void validateOssConfig() {
        if (!StringUtils.hasText(ossConfig.getEndpoint())
                || !StringUtils.hasText(ossConfig.getAccessKeyId())
                || !StringUtils.hasText(ossConfig.getAccessKeySecret())
                || !StringUtils.hasText(ossConfig.getBucketName())
                || !StringUtils.hasText(ossConfig.getDomain())) {
            throw new BaseException("OSS配置不完整，当前环境要求使用OSS上传");
        }
    }

    private String uploadToOss(MultipartFile file, String objectKey) {
        OSS ossClient = null;
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    inputStream,
                    metadata
            );
            ossClient.putObject(putRequest);

            String url = ossConfig.getDomain().replaceAll("/$", "") + "/" + objectKey;
            log.info("OSS upload success: {}", url);
            return url;
        } catch (IOException e) {
            log.error("OSS upload failed, objectKey={}", objectKey, e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
            if (ossClient != null) {
                try { ossClient.shutdown(); } catch (Exception ignored) {}
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BaseException(MessageConstant.FILE_TOO_LARGE);
        }
        String suffix = extractSuffix(file.getOriginalFilename());
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String extractSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
