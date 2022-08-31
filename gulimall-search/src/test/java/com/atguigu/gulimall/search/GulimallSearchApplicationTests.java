package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;

import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallSearchApplicationTests {
    
    
    @Autowired
    private RestHighLevelClient client;
    @Test
    public void contextLoads() {
        System.out.println(client);
    }
    
    @Test
    public void indexRequest() throws IOException {
        IndexRequest indexRequest=new IndexRequest("users");
        indexRequest.id("1");
        //indexRequest.source("userName","zhangsan","age",18,"gender","男");
        User user = new User();
        
        user.setUserName("zhangsan");
        user.setAge(18);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
    
        indexRequest.source(jsonString, XContentType.JSON);
        IndexResponse index = client.index(indexRequest,RequestOptions.DEFAULT);
        System.out.println(index);
    
    }
    
   /* @Test
    public void searchRequest() throws IOException {
    
        SearchRequest request = new SearchRequest();
        request.indices("bank");
       // SearchRequestBuilder builder=new SearchRequestBuilder();
       // request.source(builder);
    
        SearchResponse search = client.search(request, GuliMallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println();
    }*/
    
    
    
    
    
    
    
    
    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    
    }}
