package org.imokkkk;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * @author ImOkkkk
 * @date 2022/2/12 20:22
 * @since 1.0
 */

public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    public void initJedis() {
        jedis = new Jedis("127.0.0.1", 6379);
        jedis.flushDB();
    }

    @AfterEach
    public void closeJedis() {
        if (jedis != null && jedis.isConnected()) {
            jedis.close();
        }
    }

    @Test
    public void testPing() {
        String reply = jedis.ping();
        System.out.println(reply);
    }

    @Test
    public void testString() {
        System.out.println(jedis.set("k1", "v1"));
        System.out.println(jedis.set("k2", "v2"));
        System.out.println(jedis.set("k3", "v3"));
        System.out.println("删除k2:" + jedis.del("k2"));
        System.out.println("获取键k2:" + jedis.get("k2"));
        System.out.println("修改k1:" + jedis.set("k1", "111"));
        System.out.println("在k3后面加入值:" + jedis.append("k3", "end"));
        System.out.println("批量设置值:" + jedis.mset("one", "1", "two", "2"));
        System.out.println("批量获取值:" + jedis.mget("one", "two"));

        System.out.println("======新增键值对防止覆盖原有的值======");
        System.out.println(jedis.setnx("k1", "v1"));

        System.out.println("======新增键值对并设置有效时间");
        System.out.println(jedis.setex("k4", 5, "v4"));
        System.out.println(jedis.get("k4"));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(jedis.get("k4"));
        System.out.println("======获取原值并更新======");
        System.out.println(jedis.getSet("k1", "newV1"));
        System.out.println("======获取指定区间======");
        System.out.println(jedis.getrange("k1", 1, 2));
    }

    @Test
    public void testList() {
        jedis.lpush("letter", "a", "b", "c", "c", "d");
        System.out.println("letter所有元素:" + jedis.lrange("letter", 0, -1));
        System.out.println("letter 0-2区间元素:" + jedis.lrange("letter", 0, 2));
        System.out.println("删除指定元素:" + jedis.lrem("letter", 1, "c"));
        System.out.println(jedis.lrange("letter", 0, -1));
        jedis.rpush("letter", "f", "e");
        System.out.println(jedis.lpop("letter"));
        System.out.println(jedis.rpop("letter"));
        System.out.println("修改指定下标1的内容:" + jedis.lset("letter", 1, "1"));
        System.out.println("获取指定下标1的内容:" + jedis.lindex("letter", 1));
        jedis.lpush("sortedList", "3", "6", "2", "0", "7", "4");
        System.out.println(jedis.sort("sortedList"));
        System.out.println(jedis.lrange("sortedList", 0, -1));
    }

    @Test
    public void testSet() {
        jedis.sadd("eleSet", "e1", "e2", "e4", "e3", "e0", "e8", "e7", "e5");
        System.out.println(jedis.smembers("eleSet"));
        System.out.println("删除一个元素e0:" + jedis.srem("eleSet", "e0"));
        System.out.println("随机移除一个元素:" + jedis.spop("eleSet"));
        System.out.println(jedis.scard("eleSet"));
        System.out.println("e3是否包含在eleSet中:" + jedis.sismember("eleSet", "e3"));
        System.out.println(jedis.sadd("eleSet1", "e1", "e2", "e4", "e3", "e0", "e8", "e7", "e5"));
        System.out.println(jedis.sadd("eleSet2", "e1", "e2", "e4", "e3", "e0", "e8"));
        System.out.println("将eleSet1中删除e1并存入eleSet3中：" + jedis.smove("eleSet1", "eleSet3", "e1"));// 移到集合元素
        System.out.println("======集合运算======");
        System.out.println("eleSet1和eleSet2的交集:" + jedis.sinter("eleSet1", "eleSet2"));
        System.out.println("eleSet1和eleSet2的并集:" + jedis.sunion("eleSet1", "eleSet2"));
        System.out.println("eleSet1和eleSet2的差集:" + jedis.sdiff("eleSet1", "eleSet2"));
        System.out.println("求交集并将交集保存到dstkey的集合:" + jedis.sinterstore("eleSet4", "eleSet1", "eleSet2"));
    }

    @Test
    public void testHash() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");
        jedis.hmset("hash", map);
        jedis.hset("hash", "key5", "value5");
        System.out.println(jedis.hgetAll("hash"));
        System.out.println(jedis.hkeys("hash"));
        System.out.println(jedis.hvals("hash"));
        System.out.println("将key6保存的值加上一个整数，如果key6不存在则添加key6:" + jedis.hincrBy("hash", "key6", 6));
        System.out.println(jedis.hdel("hash", "key2"));
        System.out.println(jedis.hlen("hash"));
        System.out.println(jedis.hexists("hash", "key2"));
        System.out.println(jedis.hmget("hash", "key2", "key3"));
    }

    @Test
    public void testMulti() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "aaa");
        jsonObject.put("age", 18);
        // 开启事务
        Transaction multi = jedis.multi();
        try {
            String jsonString = jsonObject.toJSONString();
            // 乐观锁监控
            jedis.watch(jsonString);
            multi.set("user1", jsonString);
            multi.set("user2", jsonString);
            // 模拟异常
            int i = 1 / 0;
            // 执行事务
            multi.exec();
        } catch (Exception e) {
            // 放弃事务
            multi.discard();
        } finally {
            System.out.println(jedis.get("user1"));
            System.out.println(jedis.get("user2"));
            // 关闭连接
            jedis.close();
        }
    }
}
