package com.atguigu.gulimall.product.feign;

/**
 * @author Ykj
 * @date 2022/9/6/17:18
 * @apiNote
 */

//@FeignClient(value = "gulimall-seckill",fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {
    /*@GetMapping(value = "/sku/seckill/{skuId}")
    R getSkuSeckilInfo(@PathVariable("skuId") Long skuId);*/
}
