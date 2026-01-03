package com.example.travelaibackend.component;

import cn.hutool.dfa.WordTree;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SensitiveFilter {

    private final WordTree sensitiveTree = new WordTree();

    // 初始化敏感词库
    @PostConstruct
    public void init() {
        // 实际项目中，这里可以从数据库或 TXT 文件读取
        sensitiveTree.addWord("笨蛋");
        sensitiveTree.addWord("垃圾");
        sensitiveTree.addWord("骗子");
        sensitiveTree.addWord("暴力");
        sensitiveTree.addWord("色情");
        sensitiveTree.addWord("赌博");
        // ... 添加更多
        System.out.println(">>> 敏感词过滤器初始化完成");
    }

    /**
     * 检查是否包含敏感词
     * @param text 待检测文本
     * @return true=包含, false=不包含
     */
    public boolean hasSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return sensitiveTree.isMatch(text);
    }

    /**
     * 获取文本中的第一个敏感词（用于提示用户）
     * @param text 待检测文本
     * @return 敏感词字符串，无则返回 null
     */
    public String findFirstSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return sensitiveTree.match(text);
    }
}