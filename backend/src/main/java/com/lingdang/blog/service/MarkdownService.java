package com.lingdang.blog.service;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Markdown 处理服务
 */
@Slf4j
@Service
public class MarkdownService {
    
    private final Parser parser;
    private final HtmlRenderer renderer;
    
    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create()
        ));
        
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }
    
    /**
     * Markdown 转 HTML（带 sanitize）
     */
    public String markdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        
        // 解析 Markdown
        Document document = parser.parse(markdown);
        
        // 渲染为 HTML
        String html = renderer.render(document);
        
        // Sanitize HTML（防止 XSS）
        return sanitizeHtml(html);
    }
    
    /**
     * HTML 消毒（移除危险标签和属性）
     */
    public String sanitizeHtml(String html) {
        Safelist safelist = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "hr",
                     "strong", "em", "u", "s", "code", "pre", "blockquote",
                     "ul", "ol", "li", "table", "thead", "tbody", "tr", "th", "td",
                     "a", "img")
            .addAttributes("a", "href", "title", "id")
            .addAttributes("img", "src", "alt", "title")
            .addAttributes("h1", "id")
            .addAttributes("h2", "id")
            .addAttributes("h3", "id")
            .addAttributes("h4", "id")
            .addAttributes("h5", "id")
            .addAttributes("h6", "id")
            .addAttributes("code", "class")
            .addAttributes("pre", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");
        
        return Jsoup.clean(html, safelist);
    }
    
    /**
     * 生成标题锚点（slug）
     */
    public String generateAnchor(String heading) {
        if (heading == null || heading.trim().isEmpty()) {
            return "";
        }
        
        return heading.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\s-]", "") // 保留字母、数字、中文、空格、连字符
            .replaceAll("\\s+", "-") // 空格转连字符
            .replaceAll("-+", "-") // 多个连字符合并
            .replaceAll("^-|-$", ""); // 移除首尾连字符
    }
}
