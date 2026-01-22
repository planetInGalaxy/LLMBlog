package com.lingdang.blog;

import com.lingdang.blog.model.BlogPost;
import com.lingdang.blog.repository.BlogPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器 - 自动创建示例博客文章
 */
@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private BlogPostRepository blogPostRepository;
    
    @Override
    public void run(String... args) {
        // 检查数据库是否已有数据
        if (blogPostRepository.count() > 0) {
            System.out.println("=================================");
            System.out.println("数据库已有 " + blogPostRepository.count() + " 篇文章，跳过初始化");
            System.out.println("=================================");
            return;
        }
        
        // 创建示例文章
        BlogPost post1 = new BlogPost();
        post1.setTitle("欢迎来到铃铛师兄大模型博客");
        post1.setAuthor("铃铛师兄");
        post1.setSummary("这是铃铛师兄大模型的第一篇博客文章，分享AI和大模型的最新技术和见解。");
        post1.setContent("# 欢迎来到铃铛师兄大模型\n\n" +
                "这是一个专注于人工智能和大模型技术的个人博客。在这里，我会分享：\n\n" +
                "- 大模型技术的最新进展\n" +
                "- AI应用开发经验\n" +
                "- 机器学习实践案例\n" +
                "- 技术思考和见解\n\n" +
                "欢迎大家关注和交流！");
        post1.setTags("AI,大模型,欢迎");
        post1.setPublished(true);
        blogPostRepository.save(post1);
        
        BlogPost post2 = new BlogPost();
        post2.setTitle("大模型技术入门指南");
        post2.setAuthor("铃铛师兄");
        post2.setSummary("从零开始了解大模型技术，包括基础概念、应用场景和学习路径。");
        post2.setContent("# 大模型技术入门指南\n\n" +
                "## 什么是大模型？\n\n" +
                "大模型（Large Language Model, LLM）是指参数量巨大的深度学习模型，" +
                "通过在海量文本数据上训练，具备强大的语言理解和生成能力。\n\n" +
                "## 主要应用场景\n\n" +
                "1. 智能对话和客服\n" +
                "2. 内容创作和写作辅助\n" +
                "3. 代码生成和编程助手\n" +
                "4. 知识问答和信息检索\n" +
                "5. 文本分析和理解\n\n" +
                "## 学习建议\n\n" +
                "- 掌握Python编程基础\n" +
                "- 了解机器学习基本概念\n" +
                "- 熟悉Transformer架构\n" +
                "- 实践使用主流大模型API\n");
        post2.setTags("大模型,AI,教程");
        post2.setPublished(true);
        blogPostRepository.save(post2);
        
        BlogPost post3 = new BlogPost();
        post3.setTitle("如何搭建个人AI应用");
        post3.setAuthor("铃铛师兄");
        post3.setSummary("分享使用Spring Boot和React搭建AI应用的实战经验。");
        post3.setContent("# 如何搭建个人AI应用\n\n" +
                "## 技术栈选择\n\n" +
                "### 后端\n" +
                "- Spring Boot 3.x\n" +
                "- JDK 17\n" +
                "- MySQL数据库\n\n" +
                "### 前端\n" +
                "- React 19\n" +
                "- Vite构建工具\n" +
                "- 现代化UI组件库\n\n" +
                "## 部署方案\n\n" +
                "使用Docker容器化部署，可以轻松部署到：\n" +
                "- 阿里云\n" +
                "- 腾讯云\n" +
                "- 其他云服务商\n\n" +
                "## 最佳实践\n\n" +
                "1. 前后端分离架构\n" +
                "2. RESTful API设计\n" +
                "3. 响应式Web设计\n" +
                "4. Docker容器化部署\n");
        post3.setTags("开发,Spring Boot,React");
        post3.setPublished(true);
        blogPostRepository.save(post3);
        
        System.out.println("=================================");
        System.out.println("铃铛师兄大模型博客系统启动成功！");
        System.out.println("已初始化 " + blogPostRepository.count() + " 篇示例文章");
        System.out.println("=================================");
    }
}
