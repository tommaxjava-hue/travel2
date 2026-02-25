package com.example.travelaibackend.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.ResultCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@CrossOrigin
public class FileController {

    // 文件存储根目录 (项目根目录下的 files 文件夹)
    private final String fileUploadPath = System.getProperty("user.dir") + File.separator + "files" + File.separator;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "上传文件不能为空");
        }

        // 1. 获取原文件名和后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);

        // 2. 校验文件格式 (仅允许图片)
        if (!StrUtil.equalsAnyIgnoreCase(suffix, "jpg", "jpeg", "png", "gif", "webp")) {
            return Result.error(ResultCode.VALIDATE_FAILED.getCode(), "仅支持 jpg/png/gif/webp 格式的图片");
        }

        // 3. 生成唯一文件名 (UUID + 后缀)，防止重名覆盖
        String fileName = IdUtil.fastSimpleUUID() + "." + suffix;
        File saveFile = new File(fileUploadPath + fileName);

        // 4. 确保父目录存在
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }

        // 5. 保存文件到本地
        file.transferTo(saveFile);

        // 6. 返回可供前端访问的网络 URL
        // 注意：这里假设你的后端服务运行在 localhost:8080，且 WebConfig 中已配置了 /files/** 的资源映射
        String url = "http://localhost:8080/files/" + fileName;
        return Result.success(url);
    }
}