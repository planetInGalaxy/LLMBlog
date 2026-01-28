package com.lingdang.blog.service;

import com.lingdang.blog.model.Article;
import com.lingdang.blog.model.ArticleSummaryJob;
import com.lingdang.blog.repository.ArticleRepository;
import com.lingdang.blog.repository.ArticleSummaryJobRepository;
import com.lingdang.blog.dto.llm.ChatCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ArticleSummaryJobService {

    private static final int CONTENT_CHAR_LIMIT = 15000;
    private static final int SUMMARY_MIN_CHARS = 25;
    private static final int SUMMARY_MAX_CHARS = 60;
    private static final int SUMMARY_RETRY_MAX = 2;

    @Autowired
    private ArticleSummaryJobRepository jobRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private LlmService llmService;

    /**
     * 提交摘要生成任务。
     */
    @Transactional
    public ArticleSummaryJob submit(Long articleId, ArticleSummaryJob.Mode mode) {
        if (articleId == null) {
            throw new IllegalArgumentException("articleId 不能为空");
        }

        // REGENERATE：取消旧的 pending/running，确保只跑最新的
        if (mode == ArticleSummaryJob.Mode.REGENERATE) {
            List<ArticleSummaryJob> olds = jobRepository.findByArticleIdAndStatusIn(
                articleId,
                List.of(ArticleSummaryJob.Status.PENDING, ArticleSummaryJob.Status.RUNNING)
            );
            for (ArticleSummaryJob old : olds) {
                old.setStatus(ArticleSummaryJob.Status.CANCELED);
                old.setCompletedAt(LocalDateTime.now());
                old.setErrorMessage("replaced by a newer REGENERATE job");
                jobRepository.save(old);
            }
        } else {
            // FILL_IF_EMPTY：如果已经有 pending/running 就不重复提交
            boolean exists = jobRepository.existsByArticleIdAndStatusIn(
                articleId,
                List.of(ArticleSummaryJob.Status.PENDING, ArticleSummaryJob.Status.RUNNING)
            );
            if (exists) {
                return null;
            }
        }

        ArticleSummaryJob job = new ArticleSummaryJob();
        job.setArticleId(articleId);
        job.setMode(mode);
        job.setStatus(ArticleSummaryJob.Status.PENDING);
        return jobRepository.save(job);
    }

    /**
     * Worker：定时拉取一个待处理任务，生成摘要并写回。
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void runOnce() {
        ArticleSummaryJob job = jobRepository.findFirstByStatusOrderByCreatedAtAsc(ArticleSummaryJob.Status.PENDING);
        if (job == null) return;

        // 标记 RUNNING
        job.setStatus(ArticleSummaryJob.Status.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            Article article = articleRepository.findById(job.getArticleId())
                .orElseThrow(() -> new RuntimeException("文章不存在: " + job.getArticleId()));

            String summary = generateOneLineSummary(article);
            summary = normalizeAndValidate(summary);
            // 如果不满足字数要求，则让大模型重写（最多重试 SUMMARY_RETRY_MAX 次）
            int tries = 0;
            while (!isLengthOk(summary) && tries < SUMMARY_RETRY_MAX) {
                summary = rewriteSummaryToFit(article, summary);
                summary = normalizeAndValidate(summary);
                tries++;
            }

            // 写回策略：FILL_IF_EMPTY 仅当仍为空才写；REGENERATE 允许覆盖
            if (job.getMode() == ArticleSummaryJob.Mode.FILL_IF_EMPTY) {
                String current = article.getSummary();
                if (current != null && !current.trim().isEmpty()) {
                    job.setStatus(ArticleSummaryJob.Status.SUCCESS);
                    job.setCompletedAt(LocalDateTime.now());
                    job.setErrorMessage("skipped: summary already filled");
                    jobRepository.save(job);
                    return;
                }
            }

            article.setSummary(summary);
            articleRepository.save(article);

            job.setStatus(ArticleSummaryJob.Status.SUCCESS);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            jobRepository.save(job);

            log.info("文章摘要生成成功: article_id={}, job_id={}, mode={}, summary='{}'",
                job.getArticleId(), job.getId(), job.getMode(), summary);

        } catch (Exception e) {
            job.setStatus(ArticleSummaryJob.Status.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
            job.setRetryCount(job.getRetryCount() != null ? job.getRetryCount() + 1 : 1);
            jobRepository.save(job);
            log.error("文章摘要生成失败: article_id={}, job_id={}", job.getArticleId(), job.getId(), e);
        }
    }

    private String generateOneLineSummary(Article article) throws Exception {
        String title = article.getTitle() != null ? article.getTitle().trim() : "";
        String tags = article.getTags() != null ? article.getTags().trim() : "";
        String content = article.getContentMarkdown() != null ? article.getContentMarkdown() : "";
        if (content.length() > CONTENT_CHAR_LIMIT) {
            content = content.substring(0, CONTENT_CHAR_LIMIT);
        }

        String userPrompt = "请根据以下信息，为文章生成一句中文摘要（" + SUMMARY_MIN_CHARS + "~" + SUMMARY_MAX_CHARS + "字）：\n" +
            "- 要求：只输出一句话；不要换行；不要序号；不要引号；不要以‘本文将/本文主要’开头。\n" +
            "- 风格：更吸引人但不标题党；面向大模型入门学习者，读起来有兴趣并愿意点进去看。\n" +
            "- 内容：优先写读者收益/能学到什么，避免空话。\n" +
            "- 标题：" + title + "\n" +
            "- 标签：" + tags + "\n" +
            "- 正文：\n" + content;

        List<ChatCompletionRequest.ChatMessage> messages = List.of(
            new ChatCompletionRequest.ChatMessage("system", "你是中文技术博客编辑，擅长写简洁、专业的一句话摘要。"),
            new ChatCompletionRequest.ChatMessage("user", userPrompt)
        );

        // maxTokens 只给很小的空间，避免跑偏
        return llmService.chatCompletion(messages, 160);
    }

    private String rewriteSummaryToFit(Article article, String previous) throws Exception {
        String title = article.getTitle() != null ? article.getTitle().trim() : "";
        String tags = article.getTags() != null ? article.getTags().trim() : "";

        String userPrompt = "请重写下面这句文章摘要，使其满足以下要求：\n" +
            "- 只输出一句中文\n" +
            "- 字数严格控制在 " + SUMMARY_MIN_CHARS + "~" + SUMMARY_MAX_CHARS + " 字\n" +
            "- 更吸引人但不标题党，面向大模型入门学习者\n" +
            "- 不要换行/序号/引号，不要以‘本文将/本文主要’开头\n" +
            "- 标题：" + title + "\n" +
            "- 标签：" + tags + "\n" +
            "原摘要：" + (previous == null ? "" : previous);

        List<ChatCompletionRequest.ChatMessage> messages = List.of(
            new ChatCompletionRequest.ChatMessage("system", "你是中文技术博客编辑，擅长把摘要写得有吸引力但不夸张。"),
            new ChatCompletionRequest.ChatMessage("user", userPrompt)
        );

        return llmService.chatCompletion(messages, 160);
    }

    private boolean isLengthOk(String t) {
        if (t == null) return false;
        int len = t.trim().length();
        return len >= SUMMARY_MIN_CHARS && len <= SUMMARY_MAX_CHARS;
    }

    private String normalizeAndValidate(String s) {
        if (s == null) return "";
        String t = s.replace("\n", " ").replace("\r", " ").trim();
        // 去掉首尾引号
        t = t.replaceAll("^[\"“”]+", "").replaceAll("[\"“”]+$", "");
        // 过长时做一次硬截断兜底（不阻塞）
        if (t.length() > SUMMARY_MAX_CHARS) {
            t = t.substring(0, SUMMARY_MAX_CHARS);
        }
        // 太短也允许通过（避免卡住），但尽量保证不是空
        if (t.isEmpty()) {
            t = "…";
        }
        return t;
    }
}
