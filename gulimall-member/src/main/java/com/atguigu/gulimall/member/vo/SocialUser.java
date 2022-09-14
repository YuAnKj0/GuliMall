/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.atguigu.gulimall.member.vo;

import lombok.Data;

/**
 * @author Ykj
 * @date 2022/9/11/10:18
 * @apiNote
 */

@Data
public class SocialUser {
    
    private String access_token;
    
    private String remind_in;
    
    private long expires_in;
    
    private String uid;
    
    private String isRealName;
}
