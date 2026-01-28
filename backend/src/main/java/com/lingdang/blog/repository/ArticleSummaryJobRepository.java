package com.lingdang.blog.repository;

import com.lingdang.blog.model.ArticleSummaryJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleSummaryJobRepository extends JpaRepository<ArticleSummaryJob, Long> {

    boolean existsByArticleIdAndStatusIn(Long articleId, List<ArticleSummaryJob.Status> statuses);

    List<ArticleSummaryJob> findByArticleIdAndStatusIn(Long articleId, List<ArticleSummaryJob.Status> statuses);

    ArticleSummaryJob findFirstByStatusOrderByCreatedAtAsc(ArticleSummaryJob.Status status);
}
