package com.atguigu.common.to;

import lombok.Data;

/**
 * @author Ykj
 * @date 2022/8/27/15:36
 * @apiNote
 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
