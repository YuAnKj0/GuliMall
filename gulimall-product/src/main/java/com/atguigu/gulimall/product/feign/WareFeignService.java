package com.atguigu.gulimall.product.feign;


import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author Ykj
 * @date 2022/8/27/16:41
 * @apiNote
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {
    
    
    @PostMapping("/ware/waresku/hasstock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds);
}
