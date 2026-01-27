const setMetaTag = (attribute, key, content) => {
  if (typeof document === 'undefined' || !content) return;
  const selector = `meta[${attribute}="${key}"]`;
  let element = document.querySelector(selector);
  if (!element) {
    element = document.createElement('meta');
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.setAttribute('content', content);
};

export const getPageUrl = (pathname = '') => {
  if (typeof window === 'undefined') return pathname;
  return `${window.location.origin}${pathname}`;
};

export const updateSeoTags = ({ title, description, type, url }) => {
  if (typeof document === 'undefined') return;
  if (title) document.title = title;
  if (description) setMetaTag('name', 'description', description);
  if (title) setMetaTag('property', 'og:title', title);
  if (description) setMetaTag('property', 'og:description', description);
  if (type) setMetaTag('property', 'og:type', type);
  if (url) setMetaTag('property', 'og:url', url);
};
