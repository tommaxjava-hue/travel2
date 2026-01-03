package com.example.travelaibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.travelaibackend.common.Result;
import com.example.travelaibackend.component.SensitiveFilter;
import com.example.travelaibackend.entity.Comment;
import com.example.travelaibackend.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/comment")
@CrossOrigin
public class CommentController {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

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

        return Result.success("评论成功");
    }

    // 3. 管理员删除评论
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = commentService.removeById(id);
        return success ? Result.success("评论已删除") : Result.error("500", "删除失败");
    }

    // 4. 管理员获取所有评论 (解决 Req 7 - MyBatisPlus 标准写法)
    @GetMapping("/all")
    public Result<List<Comment>> getAllComments() {
        QueryWrapper<Comment> query = new QueryWrapper<>();
        query.orderByDesc("create_time");
        return Result.success(commentService.list(query));
    }
}