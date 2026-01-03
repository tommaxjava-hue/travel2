package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@CrossOrigin
public class UploadController {

    // 图片存储路径：项目根目录下的 files 文件夹
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/files/";

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("400", "文件不能为空");
        }

        // 1. 确保目录存在
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. 生成唯一文件名 (防止重名覆盖)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 3. 保存文件到本地
        File dest = new File(UPLOAD_DIR + newFileName);
        file.transferTo(dest);

        // 4. 返回可访问的 URL
        // 假设后端端口是 8080，映射路径是 /files/
        String url = "http://localhost:8080/files/" + newFileName;
        return Result.success(url);
    }
}