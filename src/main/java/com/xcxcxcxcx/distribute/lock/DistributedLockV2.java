package com.xcxcxcxcx.distribute.lock;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 使用lua脚本，基于setNx和expire命令实现
 * @author XCXCXCXCX
 * @since 1.0
 */
public class DistributedLockV2 implements Lock{

    private static final String EXCLUSIVE_VALUE = "1";

    private final String key;

    /**
     * 锁超时时间
     */
    private final Duration timeout;

    private volatile Thread owner;

    private final StringRedisTemplate redisTemplate;

    private final String sha;

    private static final String LUA = "local flag = redis.call(\"setnx\", KEYS[1],ARGV[1]) " +
            "if(flag == 1) then " +
            "flag = redis.call(\"expire\", KEYS[1], ARGV[2]) end " +
            "return flag";

    public DistributedLockV2(String key, StringRedisTemplate redisTemplate) {
        this(key,Duration.ofSeconds(60),redisTemplate);
    }

    public DistributedLockV2(String key, Duration timeout, StringRedisTemplate redisTemplate) {
        this.key = key;
        this.timeout = timeout;
        this.redisTemplate = redisTemplate;
        this.sha = initScript();
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
        if(setNxAndExpire(EXCLUSIVE_VALUE, timeout.getSeconds())){
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

    /**
     * lua脚本保证原子操作
     * @param value
     * @param expire
     * @return
     */
    private boolean setNxAndExpire(String value, long expire){
        return redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                long returnVal = 0;
                if(connection instanceof JedisClusterConnection){
                    List<byte[]> args = new ArrayList<byte[]>();
                    args.add(value.getBytes());
                    args.add(String.valueOf(expire).getBytes());
                    returnVal = ((JedisClusterConnection) connection).execute(LUA, key.getBytes(), args);
                }else{
                    byte[][] keysAndArgs = new byte[3][];
                    keysAndArgs[0] = key.getBytes();
                    keysAndArgs[1] = value.getBytes();
                    keysAndArgs[2] = String.valueOf(expire).getBytes();
                    returnVal = connection.scriptingCommands().evalSha(sha, ReturnType.INTEGER, 1, keysAndArgs);
                }
                return returnVal == 1 ? Boolean.TRUE : Boolean.FALSE;
            }
        });
    }

    private String initScript(){
        return redisTemplate.execute(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {

                if(connection instanceof JedisClusterConnection){
                    return null;
                }
                return connection.scriptingCommands()
                        .scriptLoad((LUA).getBytes());
            }
        });
    }

    private void del(){
        redisTemplate.delete(key);
    }

    public void setOwner(Thread owner) {
        this.owner = owner;
    }
}
