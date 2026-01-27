const SUMMARY_LENGTH = 80;
const DESCRIPTION_LENGTH = 150;

const stripMarkdown = (markdown = '') => {
  let text = String(markdown);

  text = text.replace(/```[\s\S]*?```/g, ' ');
  text = text.replace(/`[^`]*`/g, ' ');
  text = text.replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1');
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
  text = text.replace(/^\s{0,3}#{1,6}\s+/gm, '');
  text = text.replace(/^\s{0,3}>\s?/gm, '');
  text = text.replace(/^\s*([-*+]|\d+\.)\s+/gm, '');
  text = text.replace(/(\*\*|__)(.*?)\1/g, '$2');
  text = text.replace(/(\*|_)(.*?)\1/g, '$2');
  text = text.replace(/~~(.*?)~~/g, '$1');
  text = text.replace(/<\/?[^>]+>/g, ' ');
  text = text.replace(/\s+/g, ' ').trim();

  return text;
};

export const getArticleSummary = (article) => {
  const summary = (article?.summary || '').trim();
  if (summary) return summary;

  const fallback = stripMarkdown(article?.contentMarkdown || '');
  if (!fallback) return '暂无摘要';
  return fallback.slice(0, SUMMARY_LENGTH);
};

export const getArticleDescription = (article) => {
  const summary = (article?.summary || '').trim();
  const fallback = stripMarkdown(article?.contentMarkdown || '');
  const base = summary || fallback;
  if (!base) return '铃铛师兄大模型博客文章分享。';
  return base.slice(0, DESCRIPTION_LENGTH);
};
