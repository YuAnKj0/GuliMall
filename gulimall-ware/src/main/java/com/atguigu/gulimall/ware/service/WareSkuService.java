package com.atguigu.gulimall.ware.service;

import com.atguigu.gulimall.ware.vo.SkuHasStockVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * εεεΊε­
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:59:40
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);
    
    
    List<SkuHasStockVO> getSkuHasStock(List<Long> skuIds);
}

