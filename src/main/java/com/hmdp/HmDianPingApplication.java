package com.hmdp;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import javafx.scene.effect.Bloom;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.hmdp.mapper")
public class HmDianPingApplication {
    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

}
