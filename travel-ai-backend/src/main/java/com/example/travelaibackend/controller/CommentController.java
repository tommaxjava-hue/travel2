package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.common.SensitiveFilter;
import com.example.travelaibackend.entity.Comment;
import com.example.travelaibackend.entity.SysUser;
import com.example.travelaibackend.service.ICommentService;
import com.example.travelaibackend.service.IUserService;
import com.example.travelaibackend.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/comment")
@CrossOrigin
public class CommentController {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IUserService userService;

    // 1. 获取某景点的评论列表
    @GetMapping("/list")
    public Result<List<Comment>> getList(Long spotId) {
        return Result.success(commentService.getCommentsBySpotId(spotId));
    }

    // 2. 发表评论
    @PostMapping("/add")
    public Result<?> addComment(@RequestBody Comment comment) {
        String content = comment.getContent();
        if (content == null || content.trim().isEmpty()) {
            return Result.error("400", "内容不能为空");
        }

        String badWord = sensitiveFilter.findFirstSensitiveWord(content);
        if (badWord != null) {
            return Result.error("403", "评论包含违规内容：" + badWord);
        }

        comment.setCreateTime(LocalDateTime.now());
        if (comment.getUserId() == null) comment.setUserId(1L);
        commentService.save(comment);

        if (comment.getSpotId() != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String sql = "UPDATE attraction SET rating = (SELECT ROUND(AVG(score), 1) FROM comment WHERE spot_id = ?) WHERE spot_id = ?";
                    jdbcTemplate.update(sql, comment.getSpotId(), comment.getSpotId());
                } catch (Exception e) {
                    System.err.println("异步更新景点评分失败：" + e.getMessage());
                }
            });
        }

        return Result.success("评论成功");
    }

    // 3. 管理员删除评论
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        SysUser user = userService.getById(userId);
        boolean isAdmin = user != null && "admin".equals(user.getRole());

        // 【强制业务约束】：业务层兜底校验，允许普通管理员 或 最高统帅(ID:88)
        if (userId == null || (userId != 88L && !isAdmin)) {
            return Result.error("403", "业务层越权拦截：清理社区违规评论仅管理员及最高统帅(ID:88)可用");
        }

        boolean success = commentService.removeById(id);
        return success ? Result.success("评论已删除") : Result.error("500", "删除失败");
    }

    // 4. 获取所有评论
    @GetMapping("/all")
    public Result<List<Comment>> getAllComments() {
        QueryWrapper<Comment> query = new QueryWrapper<>();
        query.orderByDesc("create_time");
        return Result.success(commentService.list(query));
    }
}