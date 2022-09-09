package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Ykj
 * @date 2022/9/6/15:33
 * @apiNote
 */

@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    
    private String attrName;
    
    private List<AttrValueWithSkuIdVo> attrValues;
    
}
