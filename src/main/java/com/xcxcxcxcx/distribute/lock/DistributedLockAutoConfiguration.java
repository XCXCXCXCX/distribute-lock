package com.xcxcxcxcx.distribute.lock;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;


/**
 * @author XCXCXCXCX
 * @since 1.0
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public DistributedLockFactory distributedLockFactory(StringRedisTemplate redisTemplate){
        return new DistributedLockFactory(redisTemplate);
    }

}
