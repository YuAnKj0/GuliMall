/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.atguigu.gulimall.member.excepition;

/**
 * @author Ykj
 * @date 2022/9/9/17:08
 * @apiNote
 */
public class PhoneExitException extends RuntimeException{
    public PhoneExitException(){
        super("该手机号已经注册");
    }
}
