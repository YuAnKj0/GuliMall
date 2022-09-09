package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Ykj
 * @date 2022/9/6/15:34
 * @apiNote
 */
@Data
public class SpuItemAttrGroupVo {
    private String groupName;
    
    private List<Attr> attrs;
    
}
