package com.example.travelaibackend.controller;

import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.SensitiveFilter;
import com.example.travelaibackend.entity.Post;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.IPostService;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.UserContext;
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

    @Autowired
    private IUserService userService;

    // 获取列表
    @GetMapping("/list")
    public Result<List<Post>> list() {
        return Result.success(postService.getPostList());
    }

    // 发布攻略
    @PostMapping("/add")
    public Result<?> add(@RequestBody Post post) {
        String badWordTitle = sensitiveFilter.findFirstSensitiveWord(post.getTitle());
        if (badWordTitle != null) {
            return Result.error("403", "标题包含违规内容：" + badWordTitle);
        }
        String badWordContent = sensitiveFilter.findFirstSensitiveWord(post.getContent());
        if (badWordContent != null) {
            return Result.error("403", "正文包含违规内容：" + badWordContent);
        }

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

    // 删除攻略
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("401", "请先登录");
        }

        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("404", "攻略不存在");
        }

        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        // 【强制业务约束】：允许作者本人、普通管理员 或 最高统帅(ID:88) 强制删除
        if (!userId.equals(post.getUserId()) && userId != 88L && !isAdmin) {
            return Result.error("403", "业务层越权拦截：您无权删除他人游记，社区治理仅管理员及最高统帅(ID:88)可用");
        }

        boolean success = postService.removeById(id);
        return success ? Result.success("攻略已删除") : Result.error("500", "删除失败");
    }
}