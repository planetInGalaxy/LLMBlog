import { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { API_URL } from '../lib/api';

const ASSISTANT_REQUEST_TIMEOUT_MS = 60000;
const CITATION_QUOTE_COLLAPSE_THRESHOLD = 140;

function CitationItem({ cite, index }) {
  const quoteText = (cite?.quote || '').trim();
  const shouldClamp = quoteText.length > CITATION_QUOTE_COLLAPSE_THRESHOLD;
  const [expanded, setExpanded] = useState(false);
  const hasScore = typeof cite?.score === 'number' && !Number.isNaN(cite.score);

  return (
    <div className="citation-card">
      <span className="citation-ref-index">[{cite.refIndex || (index + 1)}]</span>
      <a href={cite.url} target="_blank" rel="noopener noreferrer">
        <strong>{cite.title}</strong>
      </a>
      {hasScore && (
        <span className="citation-score">相关度: {(cite.score * 100).toFixed(0)}%</span>
      )}
      {quoteText && (
        <div className="citation-quote-block">
          <p className={`citation-quote${shouldClamp && !expanded ? ' is-clamped' : ''}`}>
            "{quoteText}"
          </p>
          {shouldClamp && (
            <button
              type="button"
              className="citation-toggle"
              onClick={() => setExpanded(prev => !prev)}
            >
              {expanded ? '收起' : '展开'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function AssistantPage() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const scrollRafRef = useRef(null);
  const abortControllerRef = useRef(null);
  const timeoutRef = useRef(null);
  const activeAssistantIndexRef = useRef(null);
  const activeRequestIdRef = useRef(0);
  const abortReasonRef = useRef(null);

  // 规范化 Markdown：修复流式输出导致的换行缺失问题（避免把多个标题/列表粘到一行）
  // 只处理代码块之外的内容，尽量不影响 ``` fenced code block
  const normalizeMarkdown = (text) => {
    if (!text) return text;

    // 统一换行符（SSE/代理有时会带 \r\n）
    const normalized = String(text).replace(/\r\n/g, '\n').replace(/\r/g, '\n');

    // 以 ``` 为界拆分，偶数段为非代码块，奇数段为代码块内容
    const parts = normalized.split(/```/);
    for (let i = 0; i < parts.length; i += 2) {
      let t = parts[i];

      // 1) 修复 “上一行文本#### 下一节” 这种标题粘连：在非行首出现的 ##~###### 前补空行
      //    例：xxx#### 一、...  => xxx\n\n#### 一、...
      t = t.replace(/([^\n])\s*(#{2,6}\s)/g, '$1\n\n$2');

      // 2) 修复 “#### 三、xxx- 列表项” 这种标题和列表粘连：标题后强制空行
      //    例：#### 三、xxx- a  => #### 三、xxx\n\n- a
      //    注意：不要处理有序列表（\d+. ），否则可能把分段的有序列表拆成多个独立列表，导致编号看起来总从 1 开始。
      t = t.replace(/^(#{2,6}[^\n]*?)(\s*)(- )/gm, '$1\n\n$3');

      // 3) 修复 “句子- 列表项” 同行粘连：仅在同一行内插入换行，避免吃掉下一行缩进
      //    例：...。[1]。- 要点  => ...。[1]。\n- 要点
      //    注意：不要用 \s* 跨行，否则会破坏嵌套列表缩进
      t = t.replace(/([。！？.!?;；:：])[ \t]*((?:[-*+]|\d+\.)\s+)/g, '$1\n$2');

      parts[i] = t;
    }

    return parts.join('```');
  };

  const isNearBottom = () => {
    const el = messagesContainerRef.current;
    if (!el) return true;
    const threshold = 120;
    const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
    return distance < threshold;
  };

  const scrollToBottom = (behavior = 'auto') => {
    if (!messagesEndRef.current) return;

    if (scrollRafRef.current) {
      cancelAnimationFrame(scrollRafRef.current);
    }

    scrollRafRef.current = requestAnimationFrame(() => {
      try {
        messagesEndRef.current?.scrollIntoView({ behavior });
      } finally {
        scrollRafRef.current = null;
      }
    });
  };

  useEffect(() => {
    // 只有用户在底部附近时才自动滚动；否则用户在上面看历史消息会被“拉回去”。
    if (!isNearBottom()) return;

    // 流式输出时更新频繁，smooth 会导致桌面端输入框/按钮出现“抖动”观感。
    // 这里在流式期间使用 auto，结束后再用 smooth。
    const last = messages[messages.length - 1];
    const streaming = !!last?.streaming;
    scrollToBottom(streaming ? 'auto' : 'smooth');
  }, [messages]);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 480px)');
    const handleChange = (event) => setIsMobile(event.matches);
    handleChange(mediaQuery);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const markAssistantCancelled = (index) => {
    if (index === null || index === undefined) return;
    setMessages(prev => {
      if (!prev[index]) return prev;
      const updated = [...prev];
      updated[index] = {
        ...updated[index],
        content: '已取消本次回复。',
        error: false,
        streaming: false
      };
      return updated;
    });
  };

  const abortActiveStream = (reason, updateMessage = true) => {
    if (abortControllerRef.current) {
      abortReasonRef.current = reason;
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    if (updateMessage && reason === 'manual') {
      markAssistantCancelled(activeAssistantIndexRef.current);
    }
    activeAssistantIndexRef.current = null;
  };

  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortReasonRef.current = 'unmount';
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    if (abortControllerRef.current) {
      abortActiveStream('manual');
    }

    const userMessage = input.trim();
    setInput('');

    // 添加用户消息
    const newMessages = [...messages, { role: 'user', content: userMessage }];
    const assistantIndex = newMessages.length;
    setMessages([...newMessages, { role: 'assistant', content: '', citations: [], streaming: true }]);
    setLoading(true);

    const requestId = activeRequestIdRef.current + 1;
    activeRequestIdRef.current = requestId;
    activeAssistantIndexRef.current = assistantIndex;

    const controller = new AbortController();
    abortControllerRef.current = controller;
    abortReasonRef.current = null;

    timeoutRef.current = setTimeout(() => {
      abortReasonRef.current = 'timeout';
      controller.abort();
    }, ASSISTANT_REQUEST_TIMEOUT_MS);

    const applyAssistantPatch = (patch) => {
      if (activeRequestIdRef.current !== requestId) return;
      setMessages(prev => {
        if (!prev[assistantIndex]) return prev;
        const updated = [...prev];
        updated[assistantIndex] = { ...updated[assistantIndex], ...patch };
        return updated;
      });
    };

    try {
      // 构建历史对话
      const history = messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }));

      // 使用 EventSource 或 fetch 流式接收
      const response = await fetch(`${API_URL}/assistant/query/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          question: userMessage,
          mode: 'FLEXIBLE',
          history: history
        }),
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error('网络请求失败');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let fullAnswer = '';
      let citations = [];

      // 使用 ref 存储内容，减少 React 渲染次数
      let pendingUpdate = false;

      const scheduleUpdate = () => {
        if (!pendingUpdate) {
          pendingUpdate = true;
          requestAnimationFrame(() => {
            if (activeRequestIdRef.current !== requestId) {
              pendingUpdate = false;
              return;
            }
            applyAssistantPatch({ content: fullAnswer });
            pendingUpdate = false;
          });
        }
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // 解码新数据
        buffer += decoder.decode(value, { stream: true });
        // 统一处理 CRLF，避免 \r 影响 SSE/Markdown 解析
        buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

        // SSE 格式：event:xxx\ndata:xxx\n\n
        // 按双换行分割事件
        const events = buffer.split('\n\n');
        buffer = events.pop() || ''; // 保留未完成的事件

        for (const eventBlock of events) {
          if (!eventBlock.trim()) continue;

          const lines = eventBlock.split('\n');
          let eventType = 'message';
          const dataLines = [];

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              // SSE 标准：逐行 data 以 \n 连接，空行也必须保留
              dataLines.push(line.slice(5));
            }
          }

          const eventData = dataLines.join('\n');

          // 处理不同类型的事件
          if (eventType === 'message') {
            // 直接追加内容，保留原始格式
            fullAnswer += eventData;
            scheduleUpdate();
          } else if (eventType === 'citations') {
            try {
              citations = JSON.parse(eventData);
              applyAssistantPatch({ citations });
            } catch (e) {
              console.warn('解析 citations 失败:', e);
            }
          } else if (eventType === 'done') {
            applyAssistantPatch({ content: fullAnswer, streaming: false });
            if (activeRequestIdRef.current === requestId) {
              setLoading(false);
              activeAssistantIndexRef.current = null;
            }
          } else if (eventType === 'error') {
            throw new Error(eventData || '服务器错误');
          }
        }
      }

      // 确保最终状态正确
      applyAssistantPatch({ content: fullAnswer, streaming: false });
      if (activeRequestIdRef.current === requestId) {
        setLoading(false);
        activeAssistantIndexRef.current = null;
      }

    } catch (error) {
      if (activeRequestIdRef.current !== requestId) return;

      if (error.name === 'AbortError') {
        if (abortReasonRef.current === 'timeout') {
          applyAssistantPatch({
            content: '请求超时，请稍后再试。',
            error: true,
            streaming: false
          });
          setLoading(false);
          activeAssistantIndexRef.current = null;
        }
        return;
      }

      console.error('查询失败:', error);
      applyAssistantPatch({
        content: '抱歉，查询失败了，请稍后重试。',
        error: true,
        streaming: false
      });
      setLoading(false);
      activeAssistantIndexRef.current = null;
    } finally {
      if (activeRequestIdRef.current === requestId) {
        if (timeoutRef.current) {
          clearTimeout(timeoutRef.current);
          timeoutRef.current = null;
        }
        abortControllerRef.current = null;
        abortReasonRef.current = null;
      }
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="assistant-page">
      <div className="chat-header">
        <h1>🤖 AI 学习助手</h1>
        <p>基于您的文章知识库，智能回答问题</p>
      </div>

      <div className="chat-messages" ref={messagesContainerRef}>
        {messages.length === 0 && (
          <div className="welcome-message">
            <h2>👋 欢迎使用 AI 学习助手</h2>
            <p>您可以问我任何关于文章内容的问题，我会基于知识库为您解答。</p>
            <div className="example-questions">
              <p><strong>示例问题：</strong></p>
              <button onClick={() => { setInput('文章主要讲了哪些内容？'); }}>文章主要讲了哪些内容？</button>
              <button onClick={() => { setInput('大模型有哪些关键技术点？'); }}>大模型有哪些关键技术点？</button>
              <button onClick={() => { setInput('后端/前端可以转型大模型开发吗？'); }}>后端/前端可以转型大模型开发吗？</button>
            </div>
          </div>
        )}

        {messages.map((msg, idx) => (
          <div key={idx} className={`message message-${msg.role}`}>
            {msg.role === 'user' ? (
              <div className="message-content">
                <div className="message-avatar avatar-user" aria-hidden="true">
                  <span>你</span>
                </div>
                <div className="message-text">{msg.content}</div>
              </div>
            ) : (
              <div className="message-content">
                <div className="message-avatar avatar-assistant" aria-hidden="true">
                  <span>AI</span>
                </div>
                <div className="message-text">
                  {msg.error ? (
                    <p className="error-text">{msg.content}</p>
                  ) : (
                    <>
                      <div className="markdown-body">
                        <ReactMarkdown
                          remarkPlugins={[remarkGfm]}
                          components={{
                            code({ inline, className, children, ...props }) {
                              const match = /language-(\w+)/.exec(className || '');
                              return !inline && match ? (
                                <SyntaxHighlighter
                                  style={vscDarkPlus}
                                  language={match[1]}
                                  PreTag="div"
                                  {...props}
                                >
                                  {String(children).replace(/\n$/, '')}
                                </SyntaxHighlighter>
                              ) : (
                                <code className={className} {...props}>
                                  {children}
                                </code>
                              );
                            },
                            // 确保段落、标题等元素正确渲染
                            p: ({children}) => <p>{children}</p>,
                            h1: ({children}) => <h1>{children}</h1>,
                            h2: ({children}) => <h2>{children}</h2>,
                            h3: ({children}) => <h3>{children}</h3>,
                            ul: ({children}) => <ul>{children}</ul>,
                            ol: ({children}) => <ol>{children}</ol>,
                            li: ({children}) => <li>{children}</li>,
                            strong: ({children}) => <strong>{children}</strong>,
                            em: ({children}) => <em>{children}</em>,
                          }}
                        >
                          {normalizeMarkdown(msg.content || '思考中...')}
                        </ReactMarkdown>
                      </div>

                      {msg.citations && msg.citations.length > 0 && (
                        <details className="citations">
                          <summary>📚 参考文章 ({msg.citations.length})</summary>
                          <div className="citations-content">
                            {msg.citations.map((cite, i) => (
                              <CitationItem
                                key={cite.chunkId || `${idx}-cite-${i}`}
                                cite={cite}
                                index={i}
                              />
                            ))}
                          </div>
                        </details>
                      )}
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={handleSubmit} className="chat-input-form">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder={isMobile ? '输入问题...' : '输入问题... (Enter 发送，Shift+Enter 换行)'}
          rows={3}
        />
        <button type="submit" disabled={!input.trim()}>
          {loading ? '思考中...' : '发送'}
        </button>
      </form>
    </div>
  );
}

export default AssistantPage;
