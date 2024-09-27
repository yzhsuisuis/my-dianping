package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class RedisIdWorkerTest {

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(200);

    /**
     * 测试分布式ID生成器的性能，以及可用性
     */
    @Test
    public void testNextId() throws InterruptedException {
        // 使用CountDownLatch让线程同步等待
        CountDownLatch latch = new CountDownLatch(300);
        // 创建线程任务
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            // 等待次数-1
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        // 创建300个线程，每个线程创建100个id，总计生成3w个id
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        // 线程阻塞，直到计数器归0时才全部唤醒所有线程
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("生成3w个id共耗时" + (end - begin) + "ms");
    }
    @Test
    public void testNextId01() throws InterruptedException {
        long begin = System.currentTimeMillis();
        for(int i = 0;i<30000;i++)
        {
            long id = redisIdWorker.nextId("order");
            System.out.println("id = " + id);

        }
        long end = System.currentTimeMillis();
        System.out.println((end-begin)+ "ms");


    }

}
