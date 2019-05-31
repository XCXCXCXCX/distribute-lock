package com.xcxcxcxcx.distribute.lock;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 基于setNx和getSet命令实现
 * @author XCXCXCXCX
 * @since 1.0
 */
public class DistributedLock implements Lock{

    private final String key;

    /**
     * 锁超时时间
     */
    private final Duration timeout;

    private volatile Thread owner;

    private final StringRedisTemplate redisTemplate;

    public DistributedLock(String key, StringRedisTemplate redisTemplate) {
        this(key, Duration.ofSeconds(60), redisTemplate);
    }

    public DistributedLock(String key, Duration timeout, StringRedisTemplate redisTemplate) {
        this.key = key;
        this.timeout = timeout;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void lock() {
        for(;;){
            if(tryLock()){
                break;
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        for(;;){
            if(tryLock()){
                break;
            }
            if (Thread.interrupted()){
                throw new InterruptedException();
            }
        }
    }

    @Override
    public boolean tryLock() {
        long dead = buildTimeoutTimestamp();
        if(setNx(String.valueOf(dead))){
            //获取到锁
            setOwner(Thread.currentThread());
            return true;
        }
        //检测锁超时
        long timeout = get();
        long now = System.currentTimeMillis();
        long newDead = buildTimeoutTimestamp();
        if(timeout > 0 && timeout < now && getSet(String.valueOf(newDead)) < now){
            //获取到锁
            setOwner(Thread.currentThread());
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        long end = System.currentTimeMillis() + unit.toMillis(time);
        for(;;){
            if(tryLock()){
                return true;
            }
            if(System.currentTimeMillis() > end){
                return false;
            }
            if(Thread.interrupted()){
                throw new InterruptedException("Thread is interrupted, tryLock end.");
            }
        }
    }

    @Override
    public void unlock() {
        if(Thread.currentThread() == owner){
            setOwner(null);
            del();
        }else{
            throw new IllegalMonitorStateException("Only threads holding the lock can unlock.");
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Currently Condition is unsupported.");
    }

    private void setOwner(Thread owner) {
        this.owner = owner;
    }

    private boolean setNx(String value){
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    private long get(){
        String result = redisTemplate.opsForValue().get(key);
        return Long.parseLong(result == null ? "0" : result);
    }

    private long getSet(String value){
        String result = redisTemplate.opsForValue().getAndSet(key, value);
        return Long.parseLong(result == null ? "0" : result);
    }

    private void del(){
        redisTemplate.delete(key);
    }

    private long buildTimeoutTimestamp(){
        return System.currentTimeMillis() + timeout.toMillis();
    }

}
