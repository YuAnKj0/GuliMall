package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.excepition.PhoneExitException;
import com.atguigu.gulimall.member.excepition.UsernameExitException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.atguigu.gulimall.member.vo.UserRegisterVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    MemberLevelDao memberLevelDao;
    
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }
    
    @Override
    public void register(UserRegisterVo vo) {
        MemberEntity memberEntity=new MemberEntity();
        
        //设置默认等级
        MemberLevelEntity levelEntity=memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(levelEntity.getId());
    
        //设置其它的默认信息
        //检查用户名和手机号是否唯一。感知异常，异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());
        
        //密码MD5加密
        BCryptPasswordEncoder bCryptPasswordEncoder=new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setCreateTime(new Date());
        memberEntity.setGender(0);
    
        //保存数据
        this.baseMapper.insert(memberEntity);
    }
    
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //具有登录和注册逻辑
        String uid = socialUser.getUid();
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity!=null){
            //用户已经注册过，登录就可以
            //更新用户的访问令牌的时间和access_token
            MemberEntity update=new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.updateById(update);
            
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
            
        }else {
            //用户没有注册过，需要重新注册
            MemberEntity register=new MemberEntity();
            //查询社交账号的信息，昵称，年龄，电话等
            Map<String,String> query=new HashMap<>();
            query.put("access_token",socialUser.getAccess_token());
            query.put("uid", socialUser.getUid());
    
            HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
            if (response.getStatusLine().getStatusCode()==200) {
                //查询成功
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                String profileImageUrl = jsonObject.getString("profile_image_url");
                
                register.setNickname(name);
                register.setGender("m".equals(gender)?1:0);
                register.setHeader(profileImageUrl);
                register.setCreateTime(new Date());
                register.setSocialUid(socialUser.getUid());
                register.setAccessToken(socialUser.getAccess_token());
                register.setExpiresIn(socialUser.getExpires_in());
                
                //将用户信息插入数据库中
                this.baseMapper.insert(register);
            }
            return register;
        }
        
    }
    
    private void checkUserNameUnique(String userName) throws UsernameExitException{
        Integer usernameCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (usernameCount>0){
            throw new UsernameExitException();
        }
    
    }
    
    private void checkPhoneUnique(String phone) throws PhoneExitException {
        Integer mobileCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        
        if (mobileCount>0){
            throw new PhoneExitException();
        }
    }
    
}