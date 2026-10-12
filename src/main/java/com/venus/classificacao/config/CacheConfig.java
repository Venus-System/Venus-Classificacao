package com.venus.classificacao.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venus.classificacao.service.ClassificationResult;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig implements CachingConfigurer {

    public static final String CLASSIFICATIONS_CACHE = "classificacoes";

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer classificationsJsonCache(ObjectMapper objectMapper) {
        ObjectMapper cacheMapper = objectMapper.copy()
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        Jackson2JsonRedisSerializer<ClassificationResult> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(cacheMapper, ClassificationResult.class);

        return builder -> {
            RedisCacheConfiguration jsonConfig = builder.cacheDefaults()
                    .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));
            builder.withCacheConfiguration(CLASSIFICATIONS_CACHE, jsonConfig);
        };
    }
}
