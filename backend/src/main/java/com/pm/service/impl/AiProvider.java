package com.pm.service.impl;

/**
 * AI服务抽象接口
 * 支持DeepSeek和Ollama两种实现
 */
public interface AiProvider {
    /**
     * 同步调用AI，返回模型生成文本
     */
    String chat(String prompt);
}
