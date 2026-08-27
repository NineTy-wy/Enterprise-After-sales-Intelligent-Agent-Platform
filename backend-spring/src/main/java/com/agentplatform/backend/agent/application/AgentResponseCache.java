package com.agentplatform.backend.agent.application;

import com.agentplatform.backend.agent.api.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 短时响应缓存。
 *
 * <p>同一租户、同一问题和同一知识库范围内的重复查询不必重复消耗模型
 * Token。生产环境可把实现替换成 Redis，接口和缓存键策略保持不变。</p>
 */
@Component
public class AgentResponseCache {

    private final boolean enabled;
    private final Duration ttl;
    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public AgentResponseCache(
            @Value("${app.agent.cache-enabled:true}") boolean enabled,
            @Value("${app.agent.cache-ttl-seconds:30}") long ttlSeconds
    ) {
        this.enabled = enabled;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    public ChatResponse get(String key) {
        if (!enabled) {
            return null;
        }
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.createdAt().plus(ttl).isBefore(Instant.now())) {
            entries.remove(key);
            return null;
        }
        return entry.response();
    }

    public void put(String key, ChatResponse response) {
        if (enabled && response != null) {
            entries.put(key, new CacheEntry(Instant.now(), response));
        }
    }

    private record CacheEntry(Instant createdAt, ChatResponse response) {
    }
}
