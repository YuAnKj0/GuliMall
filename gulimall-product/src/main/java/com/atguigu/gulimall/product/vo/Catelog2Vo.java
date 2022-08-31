package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Ykj
 * @date 2022/8/30/17:38
 * @apiNote
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {
    
    private String catelog1Id;
    private List<Category3Vo> catelog3List;
    
    private String id;
    private String name;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category3Vo{
        private String catelog2Id;
        private String id;
        private String name;
        
    }
    
    
    
}
