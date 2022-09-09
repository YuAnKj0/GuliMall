package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Ykj
 * @date 2022/9/6/15:20
 * @apiNote
 */

@Slf4j
@Controller
public class ItemController {
    
    @Autowired
    private SkuInfoService skuInfoService;
 
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model){
        log.info("开始查询"+skuId+"信息");
        
        SkuItemVo vos=skuInfoService.item(skuId);
        
        model.addAttribute("items",vos);
        
        return "item";
        
    }
    
    
    
    
    
    
    
    
}
