package com.hmdp.service.Impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.hash.BloomFilter;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private BloomFilter<Long> bloomFilter;

//    @PostConstruct
//    public void init()
//    {
//        System.out.println("@PostConstruct--> 项目启动");
//        for(int i = 1 ;i<=14;i++)
//        {
//            System.out.println(i);
//            Shop shop = this.getById(i);
//            String prefix = CACHE_SHOP_KEY + i;
//            stringRedisTemplate.opsForValue().set(prefix, JSONUtil.toJsonStr(shop),3600L,TimeUnit.MINUTES);
//            bloomFilter.put((long) i);
//
//
//        }
//    }

    @Override
    public Result queryById(Long id) {
//       Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,
//                id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


        if(shop == null)
        {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);

    }
}
