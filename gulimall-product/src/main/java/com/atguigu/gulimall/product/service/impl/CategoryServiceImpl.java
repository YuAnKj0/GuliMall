package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());




        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     * 
     *  // @Caching(evict = {
     *     //         @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
     *     //         @CacheEvict(value = "category",key = "'getCatalogJson'")
     *     // })
     */
    @CacheEvict(value = "category",allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }
    
    /**
     * 每一个需要缓存的数据我们都来指定要放到那个名字的缓存。【缓存的分区(按照业务类型分)】
     * 代表当前方法的结果需要缓存，如果缓存中有，方法都不用调用，如果缓存中没有，会调用方法。最后将方法的结果放入缓存
     * 默认行为
     *      如果缓存中有，方法不再调用
     *      key是默认生成的:缓存的名字::SimpleKey::[](自动生成key值)
     *      缓存的value值，默认使用jdk序列化机制，将序列化的数据存到redis中
     *      默认时间是 -1：
     *
     *   自定义操作：key的生成
     *      指定生成缓存的key：key属性指定，接收一个Spel
     *      指定缓存的数据的存活时间:配置文档中修改存活时间
     *      将数据保存为json格式
     *
     *
     * 4、Spring-Cache的不足之处：
     *  1）、读模式
     *      缓存穿透：查询一个null数据。解决方案：缓存空数据
     *      缓存击穿：大量并发进来同时查询一个正好过期的数据。解决方案：加锁 ? 默认是无加锁的;使用sync = true来解决击穿问题
     *      缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间
     *  2)、写模式：（缓存与数据库一致）
     *      1）、读写加锁。
     *      2）、引入Canal,感知到MySQL的更新去更新Redis
     *      3）、读多写多，直接去数据库查询就行
     *
     *  总结：
     *      常规数据（读多写少，即时性，一致性要求不高的数据，完全可以使用Spring-Cache）：写模式(只要缓存的数据有过期时间就足够了)
     *      特殊数据：特殊设计
     *
     *  原理：
     *      CacheManager(RedisCacheManager)->Cache(RedisCache)->Cache负责缓存的读写
     * @return
     */
    @Cacheable(value = ("category"),key = "#root.methodName",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        
        return categoryEntities;
    }
    
    
    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        System.out.println("查询了数据库");
        
        //将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        
        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        
        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            
            //2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());
                    
                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            
                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(category3Vos);
                    }
                    
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            
            return catelog2Vos;
        }));
        
        return parentCid;
    }
    
    /**
     * 缓存中存储字符砖对象，但返回的是序列化对象
     * 
     * 缓存雪崩：设置过期时间（加随机值）
     * 缓存穿透：空结果缓存，设置空值
     * 缓存击穿：加锁
     * @return
     */
    @Cacheable(value = "category",key = "#root.methodName")
    public Map<String, List<Catelog2Vo>> getCatelogJson2() {
        //加入缓存逻辑,缓存存储的是JSON字符串
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (StringUtils.isEmpty(catelogJSON)){
            //魂村中没有数据，查询数据库
            Map<String, List<Catelog2Vo>> catelogJsonFromDB = getCatelogJsonFromDBWithRedisLock();
            String s = JSON.toJSONString(catelogJsonFromDB);
            //放入缓存
            redisTemplate.opsForValue().set("catelogJSON",s,1,TimeUnit.DAYS);
            return catelogJsonFromDB;
        }
        //转为指定对象
        Map<String, List<Catelog2Vo>> result=JSON.parseObject(catelogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        //TODO  堆外内存溢出 OutOfDirectMemoryError
        //lettuce的bug导致netty内存溢出，如果么有指定Xmx
        
        return result;
        
    }
    
    /**
     * 缓存数据一致性
     * 1》双写模式：数据库改完改缓存
     * 2》失效模式：删掉缓存中的数据，等待下次查询更新
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedissonLock() {
        
        /**
         *锁的名字一样就是锁一样，锁的粒度越细越快
         * 具体缓存的是某个数据，11号商品：product-11-lock
         */
        RLock lock = redisson.getLock("CatelogJSON-lock");
        lock.lock();
    
        Map<String, List<Catelog2Vo>> dataFromDB=null;
        
            try {
                //加锁成功
                //log.info("加锁成功");
                //redisTemplate.expire("lock",30,TimeUnit.SECONDS);
                dataFromDB=getDataFromDB();
                
            } finally {
                //获取值对比，+值对比成功删除=原子操作，可以使用lua脚本解锁
               /* String lockValue = redisTemplate.opsForValue().get("lock");
                if (uuid.equals(lockValue)){
                    redisTemplate.delete("lock");
                }*/
                //String script="";
                //Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
                lock.unlock();
            }
            
            return dataFromDB;
        }
        
        
    
    
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithRedisLock() {
        
        /**
         * 
         */
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111",100,TimeUnit.SECONDS);
        Map<String, List<Catelog2Vo>> dataFromDB=null;
        if (lock) {
            try {
                //加锁成功
                log.info("加锁成功");
                //redisTemplate.expire("lock",30,TimeUnit.SECONDS);
                dataFromDB=getDataFromDB();
                
            } finally {
                //获取值对比，+值对比成功删除=原子操作，可以使用lua脚本解锁
               /* String lockValue = redisTemplate.opsForValue().get("lock");
                if (uuid.equals(lockValue)){
                    redisTemplate.delete("lock");
                }*/
                String script="";
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            
            return dataFromDB;
        }else {
            //加锁失败  重试
            log.info("加锁失败，重试。。。");
            return getCatelogJsonFromDBWithRedisLock();//自旋
        }
        
    }
    
    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catelogJSON = redisTemplate.opsForValue().get("CatelogJSON");
        if (!StringUtils.isEmpty(catelogJSON)) {
            //魂村中没有数据，查询数据库
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        
        //将查询变为一次
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        
        //查出所有1级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> {
            return k.getCatId().toString();
        }, v -> {
            //查到一级分类中的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前2级分类的三级分类
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> collect = level3Catelog.stream().map(l3 -> {
                            //fenghzunag指定格式的数据
                            Catelog2Vo.Category3Vo catelog3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        
                        catelog2Vo.setCatelog3List(collect);
                        
                    }
                    
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
            
        }));
        String s = JSON.toJSONString(parent_cid);
        //放入缓存
        redisTemplate.opsForValue().set("CatelogJSON", s, 1, TimeUnit.DAYS);
        
        return parent_cid;
    }
    
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDBWithLocalLock() {
    
        /**
         * synchronized (this){} 本地锁 sringboot所有的组件在容器中都是单例的，在分布式情况下
         * 本地锁：synchronized   JUC（Lock）
         * 
         */
    synchronized (this) {
        return getDataFromDB();
    }
    }
    
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parentCid) {
        List<CategoryEntity> categoryEntities = selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
       // return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return categoryEntities;
    }
    
    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }



}