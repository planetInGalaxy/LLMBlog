package com.lingdang.blog.service;

import com.lingdang.blog.dto.studio.PromptTemplateDTO;
import com.lingdang.blog.model.PromptTemplate;
import com.lingdang.blog.repository.PromptTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class PromptTemplateService {

    @Autowired
    private PromptTemplateRepository repo;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDefaults() {
        upsertIfMissing(PromptDefaults.KEY_WITH_ARTICLES, "回答提示词：仅基于文章（含引用）",
            "当检索到高相关度文章时使用；要求回答只基于文章片段，并使用 [1][2] 角标引用来源。\n触发条件：hasArticles=true 且 returnCitations=true。",
            PromptDefaults.SYSTEM_PROMPT_WITH_ARTICLES);

        upsertIfMissing(PromptDefaults.KEY_WITH_ARTICLES_NO_CITATION, "回答提示词：仅基于文章（不引用）",
            "当检索到高相关度文章时使用；要求回答只基于文章片段，但不输出角标引用。\n触发条件：hasArticles=true 且 returnCitations=false。",
            PromptDefaults.SYSTEM_PROMPT_WITH_ARTICLES_NO_CITATION);

        upsertIfMissing(PromptDefaults.KEY_FLEXIBLE, "回答提示词：灵活模式（含引用）",
            "当未检索到高相关度文章，且开启灵活模式时使用；允许模型用自身知识补充。\n触发条件：hasArticles=false 且 isFlexibleMode=true 且 returnCitations=true。",
            PromptDefaults.SYSTEM_PROMPT_FLEXIBLE);

        upsertIfMissing(PromptDefaults.KEY_FLEXIBLE_NO_CITATION, "回答提示词：灵活模式（不引用）",
            "当未检索到高相关度文章，且开启灵活模式时使用；允许模型用自身知识补充，但不输出角标引用。\n触发条件：hasArticles=false 且 isFlexibleMode=true 且 returnCitations=false。",
            PromptDefaults.SYSTEM_PROMPT_FLEXIBLE_NO_CITATION);

        upsertIfMissing(PromptDefaults.KEY_INTRO, "固定文案：助手自我介绍",
            "当用户问“你是谁/你能做什么”等时使用。",
            PromptDefaults.ASSISTANT_INTRO);

        upsertIfMissing(PromptDefaults.KEY_INTENT, "意图识别：分类器（输出 JSON）",
            "用于判断用户输入属于问候/AI与博客相关/其它无关问题。",
            PromptDefaults.INTENT_SYSTEM_PROMPT);

        upsertIfMissing(PromptDefaults.KEY_SMALL_TALK, "闲聊回复：问候语",
            "当意图识别为 SMALL_TALK 时使用；简短友好，不引用，最后引导回大模型学习问题。",
            PromptDefaults.SMALL_TALK_SYSTEM_PROMPT);

        upsertIfMissing(PromptDefaults.KEY_OTHER, "闲聊回复：无关问题",
            "当意图识别为 OTHER 时使用；简单回答不引用，最后引导回大模型学习问题。",
            PromptDefaults.OTHER_SYSTEM_PROMPT);

        log.info("PromptTemplate 默认值检查完成");
    }

    private void upsertIfMissing(String key, String name, String desc, String content) {
        if (repo.existsById(key)) return;
        PromptTemplate t = new PromptTemplate();
        t.setPromptKey(key);
        t.setName(name);
        t.setDescription(desc);
        t.setContent(content);
        repo.save(t);
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateDTO> listAll() {
        List<PromptTemplate> all = repo.findAll();
        all.sort(Comparator.comparing(PromptTemplate::getPromptKey));
        List<PromptTemplateDTO> out = new ArrayList<>();
        for (PromptTemplate t : all) {
            PromptTemplateDTO dto = new PromptTemplateDTO();
            BeanUtils.copyProperties(t, dto);
            out.add(dto);
        }
        return out;
    }

    @Transactional
    public PromptTemplateDTO update(PromptTemplateDTO dto) {
        if (dto == null || dto.getPromptKey() == null || dto.getPromptKey().trim().isEmpty()) {
            throw new RuntimeException("promptKey 不能为空");
        }
        PromptTemplate t = repo.findById(dto.getPromptKey()).orElseGet(() -> {
            PromptTemplate nt = new PromptTemplate();
            nt.setPromptKey(dto.getPromptKey());
            nt.setName(dto.getName() != null ? dto.getName() : dto.getPromptKey());
            nt.setDescription(dto.getDescription());
            nt.setContent(dto.getContent() != null ? dto.getContent() : "");
            return nt;
        });
        if (dto.getName() != null) t.setName(dto.getName());
        if (dto.getDescription() != null) t.setDescription(dto.getDescription());
        if (dto.getContent() != null) t.setContent(dto.getContent());
        repo.save(t);

        PromptTemplateDTO out = new PromptTemplateDTO();
        BeanUtils.copyProperties(t, out);
        return out;
    }

    @Transactional(readOnly = true)
    public String getContentOrDefault(String key, String fallback) {
        return repo.findById(key).map(PromptTemplate::getContent).filter(s -> s != null && !s.isBlank()).orElse(fallback);
    }
}
