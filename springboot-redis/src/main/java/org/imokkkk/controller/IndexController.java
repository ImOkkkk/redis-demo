package org.imokkkk.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liuwy
 * @date 2023/2/27 22:15
 * @since 1.0
 */
@Slf4j
@RestController
public class IndexController {
    @Autowired private StringRedisTemplate stringRedisTemplate;

    @RequestMapping("/test_cluster")
    public void testCluster() throws InterruptedException {
        stringRedisTemplate.opsForValue().set("k1", "v1");
        System.out.println(stringRedisTemplate.opsForValue().get("k1"));
    }
}
