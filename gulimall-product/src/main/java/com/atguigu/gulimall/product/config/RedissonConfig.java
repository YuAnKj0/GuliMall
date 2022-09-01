package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Ykj
 * @date 2022/8/31/15:42
 * @apiNote
 */
@Configuration
public class RedissonConfig {
    
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        Config config=new Config();
        config.useSingleServer().setAddress("redis://192.168.66.136:6379");
        RedissonClient client= Redisson.create(config);
        return client;
    }
}
    
    
    
