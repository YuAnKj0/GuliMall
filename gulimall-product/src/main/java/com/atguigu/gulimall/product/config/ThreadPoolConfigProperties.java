package com.atguigu.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ykj
 * @date 2022/9/6/17:22
 * @apiNote
 */

@ConfigurationProperties(prefix = "gulimall.thread")
@Data
public class ThreadPoolConfigProperties {
    
    private Integer coreSize;
    
    private Integer maxSize;
    
    private Integer keepAliveTime;
    
}
