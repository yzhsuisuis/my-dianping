package com.hmdp.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;

@Configuration
public class BloomFilterConfig {

    @Value("${bloom-filter.expected-insertions}")
    private int expectedInsertions;

    @Value("${bloom-filter.fpp}")
    private double falsePositiveProbability;


    @Bean
    public BloomFilter<Long> boolmFilter()
    {
        return BloomFilter.create(Funnels.longFunnel(),1000,0.01);

    }
}
