package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.component.SensitiveFilter;
import com.example.travelaibackend.entity.Post;
import com.example.travelaibackend.service.IPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/post")
@CrossOrigin
public class PostController {

    @Autowired
    private IPostService postService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    // 获取列表
    @GetMapping("/list")
    public Result<List<Post>> list() {
        return Result.success(postService.getPostList());
    }

    // 发布攻略
    @PostMapping("/add")
    public Result<?> add(@RequestBody Post post) {
        // 1. 敏感词校验
        String badWordTitle = sensitiveFilter.findFirstSensitiveWord(post.getTitle());
        if (badWordTitle != null) {
            return Result.error("403", "标题包含违规内容：" + badWordTitle);
        }
        String badWordContent = sensitiveFilter.findFirstSensitiveWord(post.getContent());
        if (badWordContent != null) {
            return Result.error("403", "正文包含违规内容：" + badWordContent);
        }

        // 2. 保存
        post.setCreateTime(LocalDateTime.now());
        post.setViewCount(0);
        postService.save(post);
        return Result.success("发布成功");
    }

    // 详情
    @GetMapping("/detail/{id}")
    public Result<Post> detail(@PathVariable Long id) {
        return Result.success(postService.getById(id));
    }

    // 【新增】删除攻略
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = postService.removeById(id);
        return success ? Result.success("攻略已删除") : Result.error("500", "删除失败");
    }
}