package org.geekbang.time.commonmistakes.cachedesign.redistransaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TestService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    public Long test1() {
        return stringRedisTemplate.opsForValue().increment("test", 1);
    }

    public Long test2() {
        stringRedisTemplate.setEnableTransactionSupport(true);
        return stringRedisTemplate.opsForValue().increment("test", 1);
    }

    @Transactional
    public Long test3() {
        stringRedisTemplate.setEnableTransactionSupport(true);
        return stringRedisTemplate.opsForValue().increment("test", 1);
    }

    @PostConstruct
    public void monitor() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            GenericObjectPool<Jedis> jedisPool = jedisPool();
            if (jedisPool != null) {
                log.info("max:{} active:{} wait:{} idle:{}", jedisPool.getMaxTotal(), jedisPool.getNumActive(),
                        jedisPool.getNumWaiters(), jedisPool.getNumIdle());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private GenericObjectPool<Jedis> jedisPool() {
        try {
            Field pool = JedisConnectionFactory.class.getDeclaredField("pool");
            pool.setAccessible(true);
            Pool<Jedis> jedisPool = (Pool<Jedis>) pool.get(redisConnectionFactory);
            Field internalPool = Pool.class.getDeclaredField("internalPool");
            internalPool.setAccessible(true);
            return (GenericObjectPool<Jedis>) internalPool.get(jedisPool);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

}
