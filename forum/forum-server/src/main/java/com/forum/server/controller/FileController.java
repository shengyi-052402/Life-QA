package com.forum.server.controller;

import com.forum.common.result.Result;
import com.forum.server.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件上传模块")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传图片文件")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        // 为了兼容 WangEditor 上传返回值格式（如果需要在前端专门对接或者直接在拦截器处理）
        return Result.success("上传成功", data);
    }
}
