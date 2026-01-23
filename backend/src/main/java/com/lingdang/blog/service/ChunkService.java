package com.lingdang.blog.service;

import com.lingdang.blog.model.Article;
import com.lingdang.blog.model.ArticleChunk;
import com.lingdang.blog.repository.ArticleChunkRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chunk 切分服务
 */
@Slf4j
@Service
@Transactional
public class ChunkService {
    
    @Autowired
    private ArticleChunkRepository articleChunkRepository;
    
    @Autowired
    private MarkdownService markdownService;
    
    // Markdown 标题正则
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    // Chunk 大小配置
    private static final int MIN_CHUNK_SIZE = 600;
    private static final int MAX_CHUNK_SIZE = 900;
    private static final int OVERLAP_SIZE = 100;
    
    /**
     * 切分文章为 chunks
     */
    public List<ArticleChunk> splitArticle(Article article) {
        String markdown = article.getContentMarkdown();
        List<ChunkDraft> drafts = splitByHeadings(markdown);
        
        List<ArticleChunk> chunks = new ArrayList<>();
        int sequenceNumber = 1;
        
        for (ChunkDraft draft : drafts) {
            ArticleChunk chunk = new ArticleChunk();
            chunk.setChunkId(generateChunkId(article.getId(), sequenceNumber));
            chunk.setArticleId(article.getId());
            chunk.setSlug(article.getSlug());
            chunk.setTitle(article.getTitle());
            chunk.setTags(article.getTags());
            chunk.setStatus(article.getStatus());
            chunk.setIndexVersion(article.getIndexVersion());
            chunk.setHeadingLevel(draft.getHeadingLevel());
            chunk.setHeadingText(draft.getHeadingText());
            chunk.setAnchor(draft.getAnchor());
            chunk.setChunkText(draft.getChunkText());
            chunk.setTokenCount(estimateTokenCount(draft.getChunkText()));
            chunk.setSequenceNumber(sequenceNumber++);
            
            chunks.add(chunk);
        }
        
        log.info("文章切分完成: article_id={}, chunks={}", article.getId(), chunks.size());
        return chunks;
    }
    
    /**
     * 按标题切分
     */
    private List<ChunkDraft> splitByHeadings(String markdown) {
        List<ChunkDraft> chunks = new ArrayList<>();
        
        Matcher matcher = HEADING_PATTERN.matcher(markdown);
        List<HeadingInfo> headings = new ArrayList<>();
        
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2);
            int start = matcher.start();
            headings.add(new HeadingInfo(level, text, start));
        }
        
        // 如果没有标题，整篇文章作为一个 chunk
        if (headings.isEmpty()) {
            ChunkDraft chunk = new ChunkDraft();
            chunk.setHeadingLevel(0);
            chunk.setHeadingText("");
            chunk.setAnchor("");
            chunk.setChunkText(markdown);
            chunks.add(chunk);
            return chunks;
        }
        
        // 按标题切分
        for (int i = 0; i < headings.size(); i++) {
            HeadingInfo heading = headings.get(i);
            int start = heading.getStart();
            int end = (i < headings.size() - 1) ? headings.get(i + 1).getStart() : markdown.length();
            
            String chunkText = markdown.substring(start, end).trim();
            
            // 如果 chunk 太大，进一步切分
            if (estimateTokenCount(chunkText) > MAX_CHUNK_SIZE) {
                chunks.addAll(splitLargeChunk(chunkText, heading.getLevel(), heading.getText()));
            } else {
                ChunkDraft chunk = new ChunkDraft();
                chunk.setHeadingLevel(heading.getLevel());
                chunk.setHeadingText(heading.getText());
                chunk.setAnchor(markdownService.generateAnchor(heading.getText()));
                chunk.setChunkText(chunkText);
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }
    
    /**
     * 切分过大的 chunk
     */
    private List<ChunkDraft> splitLargeChunk(String text, int level, String headingText) {
        List<ChunkDraft> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;
        
        for (String para : paragraphs) {
            int paraTokens = estimateTokenCount(para);
            
            if (tokenCount + paraTokens > MAX_CHUNK_SIZE && tokenCount > MIN_CHUNK_SIZE) {
                // 保存当前 chunk
                ChunkDraft chunk = new ChunkDraft();
                chunk.setHeadingLevel(level);
                chunk.setHeadingText(headingText);
                chunk.setAnchor(markdownService.generateAnchor(headingText));
                chunk.setChunkText(currentChunk.toString().trim());
                chunks.add(chunk);
                
                // 开始新 chunk（带 overlap）
                currentChunk = new StringBuilder();
                tokenCount = 0;
            }
            
            currentChunk.append(para).append("\n\n");
            tokenCount += paraTokens;
        }
        
        // 保存最后一个 chunk
        if (currentChunk.length() > 0) {
            ChunkDraft chunk = new ChunkDraft();
            chunk.setHeadingLevel(level);
            chunk.setHeadingText(headingText);
            chunk.setAnchor(markdownService.generateAnchor(headingText));
            chunk.setChunkText(currentChunk.toString().trim());
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    /**
     * 估算 token 数量（简单估算：字符数 / 4）
     */
    private int estimateTokenCount(String text) {
        if (text == null) return 0;
        return text.length() / 4;
    }
    
    /**
     * 生成 chunk ID
     */
    private String generateChunkId(Long articleId, int sequence) {
        return String.format("chunk_%d_%03d", articleId, sequence);
    }
    
    /**
     * 保存 chunks
     */
    public void saveChunks(List<ArticleChunk> chunks) {
        articleChunkRepository.saveAll(chunks);
        log.info("保存 chunks: count={}", chunks.size());
    }
    
    /**
     * 删除文章的所有 chunks
     */
    public void deleteChunksByArticleId(Long articleId) {
        articleChunkRepository.deleteByArticleId(articleId);
        log.info("删除文章 chunks: article_id={}", articleId);
    }
    
    /**
     * 原子替换文章的所有 chunks（删除旧的 + 保存新的）
     * 用于重新索引时避免唯一键冲突
     */
    public void replaceChunks(Long articleId, List<ArticleChunk> newChunks) {
        // 1. 删除旧 chunks
        articleChunkRepository.deleteByArticleId(articleId);
        log.info("删除旧 chunks: article_id={}", articleId);
        
        // 2. 立即 flush，确保 DELETE 操作执行完成
        articleChunkRepository.flush();
        
        // 3. 保存新 chunks
        articleChunkRepository.saveAll(newChunks);
        log.info("保存新 chunks: article_id={}, count={}", articleId, newChunks.size());
    }
    
    /**
     * Chunk 草稿（内部使用）
     */
    @Data
    private static class ChunkDraft {
        private Integer headingLevel;
        private String headingText;
        private String anchor;
        private String chunkText;
    }
    
    /**
     * 标题信息（内部使用）
     */
    @Data
    private static class HeadingInfo {
        private final int level;
        private final String text;
        private final int start;
    }
}
