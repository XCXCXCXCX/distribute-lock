package com.xcxcxcxcx.distribute.lock;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public final class DistributedLockFactory {

    private final StringRedisTemplate redisTemplate;

    public DistributedLockFactory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public DistributedLock create(String key){
        return new DistributedLock(key, redisTemplate);
    }

    public DistributedLock create(String key, Duration timeout){
        return new DistributedLock(key, timeout, redisTemplate);
    }

    public DistributedLockV2 createV2(String key){
        return new DistributedLockV2(key, redisTemplate);
    }

    public DistributedLockV2 createV2(String key, Duration timeout){
        return new DistributedLockV2(key, timeout, redisTemplate);
    }

}
