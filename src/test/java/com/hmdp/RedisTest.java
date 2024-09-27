package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.google.common.hash.BloomFilter;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
public class RedisTest {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;
    @Resource
    private CacheClient cacheClient;

    @Resource
    private BloomFilter<Long> bloomFilter;
    @Test
    void test01()
    {
        Jedis jedis = new Jedis("192.168.213.135", 6378);
        jedis.auth("123456");
        jedis.set("yangbo","1234");
        jedis.get("yangbo");
        jedis.close();
    }

    /**
     * 预加载店铺数据,是逻辑过期版本的
     */
    @Test
    void test02()
    {
        for(int i = 1 ;i<=14;i++)
        {
            System.out.println(i);
            Shop shop = shopService.getById(i);
            String prefix = CACHE_SHOP_KEY + i;
            cacheClient.setWithLogicalExpire(prefix,shop,3600L, TimeUnit.SECONDS);
        }

    }
    @Test
    void test03()
    {
        for(int i = 1 ;i<=14;i++)
        {
            System.out.println(i);
            Shop shop = shopService.getById(i);
            String prefix = CACHE_SHOP_KEY + i;
            stringRedisTemplate.opsForValue().set(prefix, JSONUtil.toJsonStr(shop),3600L,TimeUnit.MINUTES);
            bloomFilter.put((long) i);


        }

    }
    @Test
    void test04()
    {
        boolean flag = bloomFilter.put(1L);
        System.out.println(flag);
        boolean contain = bloomFilter.mightContain(1L);
        System.out.println(contain);


    }
}
