package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.hash.BloomFilter;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @author yangz
 */
@Slf4j
@Component
public class CacheClient {
    @Resource
    private BloomFilter<Long> bloomFilter;

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public void setWithLogicalExpire(String key,Object value,Long time ,TimeUnit unit)
    {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));

    }
    public RedisData getRedisDataByKey(String key)
    {
        String json = stringRedisTemplate.opsForValue().get(key);
        /*
         * 这里你弄的有问题,应该是查询到了过后直接返回,没查询到才重置缓存
         * */
//        R r = getObjectFromCache(key,type);

        return JSONUtil.toBean(json, RedisData.class);
    }
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        /*
        * 1. 拼接Redis前缀,然后去数据库查找这个数据,
        * */
        String key = keyPrefix + id;

        //
        RedisData redisData = getRedisDataByKey(key);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if(Objects.nonNull(r) && expireTime.isAfter(LocalDateTime.now()))
        {
            return r;
        }
        /*
        * 注意由于这里是直接封装的一个类,所以你想要拿到数据需要先转对象,
        * 对象的属性转json ,然后json再转
        *
        * */
        //已经过期了
        String lock = LOCK_SHOP_KEY + id;
        boolean islock = tryLock(lock);
        if(islock)
        {
            //获取锁成功
            CACHE_REBUILD_EXECUTOR.submit(() ->{
                try {
                    R newR = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,newR,time,unit);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    unlock(lock);
                }
            });
        }
        //获取锁失败,则重新查询
        redisData = getRedisDataByKey(key);
        r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        if(Objects.isNull(r))
        {
            return null;
        }
        expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now()))
        {
            return r;

        }
        return r;



    }

    /**
     * 通过缓存空值,来解决大量时间的缓存穿透
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;

        //布隆过滤器判断缓存是否存在
//        if(!bloomFilter.mightContain((Long) id))
//        {
//            return null;
//
//        }

        String json = stringRedisTemplate.opsForValue().get(key);
        //这里的isNotBlank是排除null和""的
        if(StrUtil.isNotBlank(json))
        {
            //这里有个很bug的地方就是我存的是Redisdata
            R r = JSONUtil.toBean(json,type);
            return r;
        }
        //验证一下,是不是非null,
        if(Objects.nonNull(json))
        {
            //当前用户是空字符串,代表已经缓存过了
            bloomFilter.put((Long) id);
            return null;
        }
        //现在用户是null,没有被缓存
        R r = dbFallback.apply(id);
        if(Objects.isNull(r))
        {
            //查询的用户为空,需要缓存一个空字符串
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
//        现在查询出来的用户是,非空的
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        bloomFilter.put((Long) id);
        return r;
    }
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(json))
        {
            //字符串不是null 也不是空串 "";
            R r = JSONUtil.toBean(json, type);
            return r;
        }
        if(json!= null)
        {
            //这里代表缓存的是空字符串,可以搞
            return null;
        }
        R r = null;
        String lock = LOCK_SHOP_KEY + id;


            try {
                boolean isLock = tryLock(lock);
                if(!isLock) {
                    Thread.sleep(50);
                    return queryWithLogicalExpire(keyPrefix, id, type, dbFallback, time, unit);
                }
                r  = dbFallback.apply(id);
                if(r == null)
                {
                    stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                    return null;
                }
                stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),CACHE_SHOP_TTL,TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                unlock(lock);
            }



        return r;
    }
    /**
     * 获取锁
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}