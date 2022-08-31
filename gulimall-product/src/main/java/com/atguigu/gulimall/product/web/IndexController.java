package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Ykj
 * @date 2022/8/30/17:44
 * @apiNote
 */
@Controller
public class IndexController {
    
    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;
    
    
    @GetMapping(value = {"/","index.html"})
    private String indexPage(Model model) {
        
        //1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categories",categoryEntities);
        
        return "index";
    }
    
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){
        Map<String, List<Catelog2Vo>> catelogJson=categoryService.getCatelogJson();
        
        return catelogJson;
    }
    
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
    
        RReadWriteLock lock = redisson.getReadWriteLock("rw_lock");
    
        String s="";
        RLock rLock = lock.writeLock();
        try {
            //该数据加写锁，读数据加读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            
            redisTemplate.opsForValue().set("writeValue",s);
            Thread.sleep(30000);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;
    }
    
    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
    
        RReadWriteLock lock = redisson.getReadWriteLock("rw_lock");
        
        String s="";
        RLock rLock = lock.readLock();
        
        try {
            rLock.lock();
            redisTemplate.opsForValue().get("writeValue");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return s;
    }
    
    /**
     * 车库停车，信号量
     * k可以用作分布式限流
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
       // park.acquire();//获取一个信号，获取一个值，站一个车位
        boolean b = park.tryAcquire();
        return "ok="+b;
    }
    
    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();//释放一个车位
        return "ok";
    }
    
    /**
     * 闭合锁
     * 放假锁门
     * 5个班全部都走完才可以锁门
     */
    @GetMapping("/lockdoor")
    @ResponseBody
    public String lockdoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5L);
        door.await();//等待闭锁完成
    
        return "放假了。。。。";
    }
    
    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable Long id){
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();//计数减一
        
        
        return id+"班的人都走了";
        
        
    }
    
}
